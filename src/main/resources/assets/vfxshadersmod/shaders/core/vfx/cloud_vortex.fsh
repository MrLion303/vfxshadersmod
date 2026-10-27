#version 150

uniform vec3 CameraForward;
uniform vec3 CameraRight;
uniform vec3 CameraUp;
uniform vec3 VortexDirection;
uniform vec3 VortexRight;
uniform vec3 VortexUp;
uniform float TanHalfFov;
uniform float ScreenAspect;
uniform float VortexTime;

in vec2 texCoord;
out vec4 fragColor;

const float VORTEX_DISTANCE = 196.0;
const vec3 VORTEX_RADII = vec3(90.0, 74.0, 78.0);
const int RAY_STEPS = 72;

float hash31(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}

float vfxNoise3(vec3 p) {
    vec3 cell = floor(p);
    vec3 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float n000 = hash31(cell);
    float n100 = hash31(cell + vec3(1.0, 0.0, 0.0));
    float n010 = hash31(cell + vec3(0.0, 1.0, 0.0));
    float n110 = hash31(cell + vec3(1.0, 1.0, 0.0));
    float n001 = hash31(cell + vec3(0.0, 0.0, 1.0));
    float n101 = hash31(cell + vec3(1.0, 0.0, 1.0));
    float n011 = hash31(cell + vec3(0.0, 1.0, 1.0));
    float n111 = hash31(cell + vec3(1.0, 1.0, 1.0));
    float z0 = mix(mix(n000, n100, f.x), mix(n010, n110, f.x), f.y);
    float z1 = mix(mix(n001, n101, f.x), mix(n011, n111, f.x), f.y);
    return mix(z0, z1, f.z);
}

vec3 toVortexLocal(vec3 relativePosition) {
    return vec3(
        dot(relativePosition, VortexRight) / VORTEX_RADII.x,
        dot(relativePosition, VortexUp) / VORTEX_RADII.y,
        dot(relativePosition, VortexDirection) / VORTEX_RADII.z
    );
}

float cloudDensity(vec3 local, out float broad, out float billows, out float radius) {
    radius = length(local.xy);
    float angle = atan(local.y, local.x);

    // Domain warp gives the cloud banks a spiral flow instead of circular rings.
    float turn = (1.0 - clamp(radius, 0.0, 1.0)) * 5.1
               + local.z * 0.82 - VortexTime * 0.012;
    float cs = cos(turn);
    float sn = sin(turn);
    vec2 rotated = mat2(cs, -sn, sn, cs) * local.xy;
    vec3 flow = vec3(rotated * vec2(3.15, 2.72), local.z * 3.1);

    broad = vfxNoise3(flow * 1.32 + vec3(0.0, 0.0, VortexTime * 0.003));
    vec3 warpedDetail = flow * 3.85 + vec3((broad - 0.5) * 1.45,
            (0.5 - broad) * 1.1, -VortexTime * 0.006);
    billows = vfxNoise3(warpedDetail);
    float fine = vfxNoise3(warpedDetail * 1.95 + vec3(-3.7, 8.1, VortexTime * 0.012));

    float spiralBand = sin(angle * 2.35 - radius * 9.2 + local.z * 3.4 - VortexTime * 0.028);
    float armNoise = spiralBand + (billows - 0.5) * 1.0 + (fine - 0.5) * 0.48;
    float arms = mix(0.18, 1.0, smoothstep(-0.96, 0.45, armNoise));

    float cloudField = broad + (billows - 0.5) * 0.62 - (fine - 0.5) * 0.30;
    float wisps = smoothstep(0.30, 0.64, cloudField);
    float edgeRadius = 0.84 + (broad - 0.5) * 0.30
            + (billows - 0.5) * 0.34 + (fine - 0.5) * 0.18;
    float perimeter = 1.0 - smoothstep(edgeRadius - 0.12, edgeRadius + 0.16, radius);
    float depthFade = 1.0 - smoothstep(0.70 + (billows - 0.5) * 0.10,
                                        1.02, abs(local.z));
    float volumeFade = 1.0 - smoothstep(0.79, 1.0, length(local));
    float centralOpening = smoothstep(0.012, 0.19, radius);

    return wisps * perimeter * depthFade * volumeFade * centralOpening * arms;
}

void main() {
    vec2 screen = texCoord * 2.0 - 1.0;
    vec3 rayDirection = normalize(
        CameraForward
        + CameraRight * (screen.x * ScreenAspect * TanHalfFov)
        + CameraUp * (screen.y * TanHalfFov)
    );

    vec3 center = VortexDirection * VORTEX_DISTANCE;
    vec3 radiiInverse = 1.0 / VORTEX_RADII;
    vec3 rayOriginLocal = toVortexLocal(-center);
    vec3 rayDirectionLocal = vec3(
        dot(rayDirection, VortexRight) * radiiInverse.x,
        dot(rayDirection, VortexUp) * radiiInverse.y,
        dot(rayDirection, VortexDirection) * radiiInverse.z
    );

    // Exact ray/ellipsoid intersection bounds the march to the cloud volume.
    float a = dot(rayDirectionLocal, rayDirectionLocal);
    float b = dot(rayOriginLocal, rayDirectionLocal);
    float c = dot(rayOriginLocal, rayOriginLocal) - 1.0;
    float discriminant = b * b - a * c;

    // Faint atmospheric red spill is smooth and has no rectangular silhouette.
    float closestT = max(dot(center, rayDirection), 0.0);
    vec3 nearest = rayDirection * closestT - center;
    float closestRadius = length(toVortexLocal(nearest));
    // No se dibuja una corona roja de color uniforme; la luz procede del volumen de nubes.
    float haloAlpha = 0.0;

    vec3 accumulated = vec3(0.090, 0.0025, 0.009) * haloAlpha;
    float accumulatedAlpha = haloAlpha;
    float transmittance = 1.0 - haloAlpha;

    if (discriminant > 0.0 && a > 0.00001) {
        float root = sqrt(discriminant);
        float tEnter = max((-b - root) / a, 0.0);
        float tExit = (-b + root) / a;

        if (tExit > tEnter) {
            float stepLength = (tExit - tEnter) / float(RAY_STEPS);
            float jitter = fract(sin(dot(gl_FragCoord.xy, vec2(12.9898, 78.233))) * 43758.5453);

            for (int i = 0; i < RAY_STEPS; i++) {
                float t = tEnter + (float(i) + 0.25 + jitter * 0.5) * stepLength;
                vec3 relativePosition = rayDirection * t - center;
                vec3 local = toVortexLocal(relativePosition);
                float broad;
                float billows;
                float radius;
                float density = cloudDensity(local, broad, billows, radius);

                // An emissive core remains visible through gaps in the outer cloud banks.
                float core = exp(-radius * 6.7)
                           * (1.0 - smoothstep(0.30, 0.82, abs(local.z)));
                density = max(density, core * 1.15);

                if (density > 0.012) {
                    float opticalDepth = density * stepLength * 0.034;
                    float sampleAlpha = 1.0 - exp(-opticalDepth);
                    float lighting = clamp(
                        0.22 + broad * 0.88 + billows * 0.46
                        + (1.0 - radius) * 0.38 + local.y * 0.16
                        + (vfxNoise3(local * 14.63 + vec3(-3.7, 8.1, VortexTime * 0.012)) - 0.5) * 0.22,
                        0.06, 1.72
                    );

                    vec3 cloudColor = mix(
                        vec3(0.018, 0.001, 0.003),
                        vec3(0.34, 0.008, 0.012),
                        smoothstep(0.30, 0.60, broad)
                    );
                    cloudColor = mix(cloudColor, vec3(0.82, 0.038, 0.008),
                                     smoothstep(0.43, 0.68, broad) * 0.82);
                    cloudColor = mix(cloudColor, vec3(1.0, 0.19, 0.028),
                                     smoothstep(0.54, 0.78, billows) * 0.56);
                    cloudColor *= lighting;
                    cloudColor = mix(cloudColor, vec3(1.0, 0.96, 0.82),
                                     clamp(pow(core, 0.72) * 1.28, 0.0, 1.0));
                    cloudColor += vec3(0.42, 0.16, 0.035) * core;
                    cloudColor = mix(cloudColor, vec3(1.0, 0.35, 0.075),
                                     smoothstep(0.18, 0.55, core) * 0.52);

                    float contribution = transmittance * sampleAlpha;
                    accumulated += cloudColor * contribution;
                    accumulatedAlpha += contribution;
                    transmittance *= 1.0 - sampleAlpha;
                }

                if (transmittance < 0.035) break;
            }
        }
    }

    if (accumulatedAlpha < 0.008) discard;
    vec3 color = accumulated / max(accumulatedAlpha, 0.0001);
    fragColor = vec4(color, clamp(accumulatedAlpha, 0.0, 0.97));
}
