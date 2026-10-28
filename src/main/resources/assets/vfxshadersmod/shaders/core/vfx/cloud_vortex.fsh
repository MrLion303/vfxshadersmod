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

const float VORTEX_DISTANCE = 224.0;
const vec3 VORTEX_RADII = vec3(90.0, 74.0, 78.0);
const int RAY_STEPS = 16;

float hash21(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float vortexNoise2(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash21(i);
    float b = hash21(i + vec2(1.0, 0.0));
    float c = hash21(i + vec2(0.0, 1.0));
    float d = hash21(i + vec2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

float fbm2(vec2 p) {
    float value = 0.0;
    float amplitude = 0.5;
    for (int i = 0; i < 3; i++) {
        value += vortexNoise2(p) * amplitude;
        p = p * 2.02 + vec2(7.1, -3.8);
        amplitude *= 0.5;
    }
    return value / 0.875;
}

vec3 toVortexLocal(vec3 relativePosition) {
    return vec3(
        dot(relativePosition, VortexRight) / VORTEX_RADII.x,
        dot(relativePosition, VortexUp) / VORTEX_RADII.y,
        dot(relativePosition, VortexDirection) / VORTEX_RADII.z
    );
}

void main() {
    vec2 screen = texCoord * 2.0 - 1.0;
    vec3 rayDirection = normalize(
        CameraForward
        + CameraRight * (screen.x * ScreenAspect * TanHalfFov)
        + CameraUp * (screen.y * TanHalfFov)
    );

    vec3 center = VortexDirection * VORTEX_DISTANCE;
    vec3 inverseRadii = 1.0 / VORTEX_RADII;
    vec3 rayOriginLocal = toVortexLocal(-center);
    vec3 rayDirectionLocal = vec3(
        dot(rayDirection, VortexRight) * inverseRadii.x,
        dot(rayDirection, VortexUp) * inverseRadii.y,
        dot(rayDirection, VortexDirection) * inverseRadii.z
    );

    float a = dot(rayDirectionLocal, rayDirectionLocal);
    float b = dot(rayOriginLocal, rayDirectionLocal);
    float c = dot(rayOriginLocal, rayOriginLocal) - 1.0;
    float discriminant = b * b - a * c;
    if (discriminant <= 0.0 || a <= 0.00001) {
        fragColor = vec4(0.0);
        return;
    }

    float root = sqrt(discriminant);
    float tEnter = max((-b - root) / a, 0.0);
    float tExit = (-b + root) / a;
    if (tExit <= tEnter) {
        fragColor = vec4(0.0);
        return;
    }

    float stepLength = (tExit - tEnter) / float(RAY_STEPS);
    float time = VortexTime * 0.018;
    vec3 accumulated = vec3(0.0);
    float accumulatedAlpha = 0.0;
    float transmittance = 1.0;

    for (int i = 0; i < RAY_STEPS; i++) {
        float t = tEnter + (float(i) + 0.5) * stepLength;
        vec3 local = toVortexLocal(rayDirection * t - center);
        float radius = length(local.xy);
        float angle = atan(local.y, local.x);

        // Dos grandes brazos se enrollan alrededor del centro; el ruido rompe los bordes
        // en bancos de nube de diferentes tamaños en lugar de crear anillos lisos.
        // La animación desplaza capas y turbulencia a velocidades distintas: no gira
        // la nube completa como una textura plana.
        vec2 flow = vec2(
            angle * 2.35 + radius * 8.8 - time * 0.28 + local.z * 1.65,
            radius * 11.5 + local.z * 3.2 + time * 0.22
        );
        float broad = fbm2(flow * 1.35);
        float detail = fbm2(flow * 3.1 + vec2(4.7 + time * 0.13, -3.8 - time * 0.17));
        float warp = (fbm2(vec2(angle * 1.65 + radius * 4.0 - time * 0.45,
                                local.z * 2.4 + radius * 3.0)) - 0.5) * 1.7;
        float spiral = sin(angle * 2.2 + radius * 10.2 - time * 0.38
                           + warp + (broad - 0.5) * 2.2 + local.z * 0.48);
        float depthLobes = sin(local.z * 8.5 + radius * 3.0 + time * 0.18) * 0.13;
        float arms = smoothstep(-0.72, 0.58, spiral + (detail - 0.5) * 0.70 + depthLobes);

        float edgeRadius = 0.80 + (broad - 0.5) * 0.13
                         + (detail - 0.5) * 0.08 + (warp * 0.012);
        float perimeter = 1.0 - smoothstep(edgeRadius - 0.09, edgeRadius + 0.12, radius);
        float depthFade = (1.0 - smoothstep(0.70, 1.04, abs(local.z)))
                        * (0.78 + 0.22 * smoothstep(-0.85, 0.55, local.z));
        float billows = smoothstep(0.34, 0.68, broad * 0.65 + detail * 0.25
                                  + arms * 0.20 + depthLobes * 0.16);
        float core = exp(-radius * 8.0) * (1.0 - smoothstep(0.30, 0.90, abs(local.z)));
        float density = billows * perimeter * depthFade * (0.22 + arms * 0.78);
        density = max(density, core * 1.15);
        if (density < 0.012) continue;

        // Iluminación volumétrica aproximada: la cara orientada a la luz es más
        // brillante, mientras que los bordes y la parte posterior dispersan luz roja.
        vec3 volumeNormal = normalize(vec3(local.x, local.y, local.z * 0.68));
        vec3 lightDirection = normalize(vec3(-0.42, 0.72, -0.55));
        float diffuse = 0.32 + max(dot(volumeNormal, lightDirection), 0.0) * 0.68;
        float backScatter = pow(max(dot(-volumeNormal, lightDirection), 0.0), 2.0) * 0.20;
        float viewRim = pow(1.0 - abs(dot(volumeNormal, normalize(rayDirectionLocal))), 2.0) * 0.24;
        float lighting = clamp((0.28 + broad * 0.75 + detail * 0.28
                               + arms * 0.20 + (1.0 - radius) * 0.18) * diffuse
                               + backScatter + viewRim, 0.08, 1.5);
        vec3 cloudColor = mix(vec3(0.055, 0.0015, 0.004),
                              vec3(0.46, 0.012, 0.006), smoothstep(0.28, 0.62, broad));
        cloudColor = mix(cloudColor, vec3(0.96, 0.075, 0.008),
                         smoothstep(0.38, 0.72, detail) * 0.82);
        cloudColor = mix(cloudColor, vec3(1.0, 0.28, 0.025),
                         smoothstep(0.48, 0.80, broad + arms * 0.18) * 0.74);
        cloudColor = mix(cloudColor, vec3(1.0, 0.82, 0.54),
                         clamp(pow(core, 0.68) * 1.25, 0.0, 1.0));
        cloudColor *= lighting;

        float sampleAlpha = 1.0 - exp(-density * stepLength * 0.040);
        float contribution = transmittance * sampleAlpha;
        accumulated += cloudColor * contribution;
        accumulatedAlpha += contribution;
        transmittance *= 1.0 - sampleAlpha;
        if (transmittance < 0.035) break;
    }

    if (accumulatedAlpha < 0.008) {
        fragColor = vec4(0.0);
        return;
    }
    vec3 color = accumulated / max(accumulatedAlpha, 0.0001);
    fragColor = vec4(color, clamp(accumulatedAlpha, 0.0, 0.97));
}
