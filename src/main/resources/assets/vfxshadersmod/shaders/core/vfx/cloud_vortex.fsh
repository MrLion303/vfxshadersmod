#version 150

uniform float VortexTime;

in vec2 texCoord;
out vec4 fragColor;

// Ruido 3D procedural calculado directamente en el fragment shader.
float hash31(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}

float vfxNoise3(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float n000 = hash31(i);
    float n100 = hash31(i + vec3(1.0, 0.0, 0.0));
    float n010 = hash31(i + vec3(0.0, 1.0, 0.0));
    float n110 = hash31(i + vec3(1.0, 1.0, 0.0));
    float n001 = hash31(i + vec3(0.0, 0.0, 1.0));
    float n101 = hash31(i + vec3(1.0, 0.0, 1.0));
    float n011 = hash31(i + vec3(0.0, 1.0, 1.0));
    float n111 = hash31(i + vec3(1.0, 1.0, 1.0));
    float z0 = mix(mix(n000, n100, f.x), mix(n010, n110, f.x), f.y);
    float z1 = mix(mix(n001, n101, f.x), mix(n011, n111, f.x), f.y);
    return mix(z0, z1, f.z);
}

float fbm3(vec3 p) {
    float sum = 0.0;
    float amplitude = 0.5;
    for (int octave = 0; octave < 2; octave++) {
        sum += vfxNoise3(p) * amplitude;
        p = p * 2.03 + vec3(7.13, 3.71, 5.47);
        amplitude *= 0.5;
    }
    return sum / 0.75;
}

// Densidad volumétrica con flujo en espiral, ruido de baja frecuencia y erosión fina.
float cloudDensity(vec3 p, out float broad, out float detail) {
    float radius = length(p.xy);
    float angle = atan(p.y, p.x);
    float twist = angle + radius * 4.15 - VortexTime * 0.010 + p.z * 0.34;
    vec2 swirl = vec2(cos(twist), sin(twist)) * radius;
    vec3 flow = vec3(swirl * 1.45, p.z * 1.35);

    vec2 warp = vec2(
        vfxNoise3(flow * 2.15 + vec3(0.0, 0.0, VortexTime * 0.004)),
        vfxNoise3(flow * 2.15 + vec3(17.1, 5.2, -2.7))
    ) - 0.5;
    vec3 q = flow + vec3(warp * 0.52, (warp.x - warp.y) * 0.30);

    broad = fbm3(q * 3.05 + vec3(0.0, 0.0, VortexTime * 0.003));
    float billows = vfxNoise3(q * 7.6 + vec3(4.0, -2.0, VortexTime * 0.006));
    detail = vfxNoise3(q * 15.2 + vec3(-3.0, 9.0, -VortexTime * 0.008));

    float shape = smoothstep(0.405, 0.665,
            broad + (billows - 0.5) * 0.35 - (detail - 0.5) * 0.15);
    float irregularRadius = radius + (billows - 0.5) * 0.12 + (detail - 0.5) * 0.045;
    float edge = 1.0 - smoothstep(0.79, 1.36, irregularRadius);
    float verticalProfile = 1.0 - smoothstep(0.68, 1.22, abs(p.z));
    float centralFlow = smoothstep(0.025, 0.15, radius + (billows - 0.5) * 0.025);
    return shape * edge * verticalProfile * centralFlow;
}

void main() {
    vec2 uv = (texCoord * 2.0 - 1.0) * 1.10;
    float radius = length(uv);
    float time = VortexTime * 0.012;

    vec3 accumulatedColor = vec3(0.0);
    float accumulatedAlpha = 0.0;
    float transmittance = 1.0;

    const int STEPS = 12;
    const float STEP_LENGTH = 2.12 / float(STEPS);

    // La cuadrícula solo delimita el volumen: la apariencia completa se calcula por píxel.
    for (int i = 0; i < STEPS; i++) {
        float z = -1.06 + (float(i) + 0.5) * STEP_LENGTH;
        vec2 drift = vec2(sin(z * 2.2 + time), cos(z * 1.8 - time * 0.7)) * 0.035;
        vec3 position = vec3(uv + drift, z);

        float broad;
        float detail;
        float density = cloudDensity(position, broad, detail);
        if (density > 0.015) {
            float opticalDepth = density * STEP_LENGTH * 2.75;
            float sampleAlpha = 1.0 - exp(-opticalDepth);
            float erosion = vfxNoise3(position * 17.0 + vec3(1.3, 7.1, time * 0.22));

            // Sombreado interno aproximado y dispersión cálida de las nubes densas.
            float lighting = clamp(0.22 + broad * 1.28 + (1.0 - detail) * 0.26
                    + max(0.0, erosion - 0.53) * 0.34, 0.12, 1.24);
            vec3 shadowRed = vec3(0.075, 0.008, 0.014);
            vec3 deepRed = vec3(0.28, 0.022, 0.026);
            vec3 burntOrange = vec3(0.79, 0.105, 0.026);
            vec3 hotOrange = vec3(1.0, 0.32, 0.065);
            vec3 cream = vec3(1.0, 0.69, 0.38);

            vec3 cloudColor = mix(shadowRed, deepRed, smoothstep(0.27, 0.53, broad));
            cloudColor = mix(cloudColor, burntOrange, smoothstep(0.43, 0.63, broad));
            cloudColor = mix(cloudColor, hotOrange, smoothstep(0.54, 0.79, detail) * 0.50);
            cloudColor = mix(cloudColor, cream,
                    smoothstep(0.73, 0.91, broad + detail * 0.12) * 0.48);
            cloudColor *= lighting;

            float contribution = transmittance * sampleAlpha;
            accumulatedColor += cloudColor * contribution;
            accumulatedAlpha += contribution;
            transmittance *= 1.0 - sampleAlpha;
        }
        if (transmittance < 0.045) {
            break;
        }
    }

    vec3 color = accumulatedAlpha > 0.0001
            ? accumulatedColor / accumulatedAlpha
            : vec3(0.0);
    float alpha = clamp(accumulatedAlpha, 0.0, 0.91);

    // Núcleo incandescente irregular que emerge del flujo de las nubes.
    float coreNoise = fbm3(vec3(uv * 8.0, time * 0.35));
    float coreRadius = length(uv + vec2(coreNoise - 0.5, 0.5 - coreNoise) * 0.035);
    float ember = exp(-coreRadius * 7.2) * 0.24;
    float hotCore = exp(-coreRadius * 15.5) * 0.78;
    color += vec3(1.0, 0.115, 0.018) * ember;
    color = mix(color, vec3(1.0, 0.54, 0.22), hotCore * 0.62);
    alpha = max(alpha, clamp(ember + hotCore * 0.42, 0.0, 0.84));

    float fringe = fbm3(vec3(uv * 4.5, 0.6));
    float perimeter = 1.0 - smoothstep(0.88 + (fringe - 0.5) * 0.24, 1.30, radius);
    alpha *= perimeter;
    if (alpha < 0.012) {
        discard;
    }
    fragColor = vec4(max(color, vec3(0.0)), clamp(alpha, 0.0, 0.94));
}
