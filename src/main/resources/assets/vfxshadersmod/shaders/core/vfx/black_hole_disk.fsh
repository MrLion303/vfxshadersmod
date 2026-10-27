#version 150

uniform float BlackHoleDiskTime;

in vec2 texCoord;
out vec4 fragColor;

float hash21(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float noise2(vec2 p) {
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
    float amplitude = 0.55;
    for (int i = 0; i < 3; i++) {
        value += noise2(p) * amplitude;
        p = p * 2.03 + vec2(7.1, 3.7);
        amplitude *= 0.48;
    }
    return value / 0.9196;
}

void main() {
    float angle = texCoord.x * 6.28318530718;
    float radius = clamp(texCoord.y, 0.0, 1.0);
    float time = BlackHoleDiskTime * 0.012;

    // El gas gira alrededor del horizonte. Ruido deformado produce hebras y cúmulos,
    // mientras que la geometría anular le da una perspectiva 3D real.
    // Coordinadas periódicas basadas en seno/coseno: el anillo no tiene junta visible en U=0/1.
    vec2 circular = vec2(cos(angle), sin(angle));
    vec2 flow = vec2(
        circular.x * 2.4 + radius * 7.8 - time * 2.2,
        circular.y * 2.4 + radius * 11.0 + time * 0.64
    );
    vec2 warp = vec2(fbm2(flow * 1.1), fbm2(flow * 1.1 + vec2(4.7, -8.2))) - 0.5;
    float broad = fbm2(flow * 2.2 + warp * 2.1);
    float fine = fbm2(flow * 5.8 + warp.yx * 3.4);
    float thread = pow(max(0.0, sin(angle * 19.0 + fine * 8.0 - time * 3.5)), 9.0);

    float innerFade = smoothstep(0.0, 0.075, radius);
    float outerFade = 1.0 - smoothstep(0.83, 1.0, radius);
    float turbulentMass = smoothstep(0.20, 0.73, broad * 0.66 + fine * 0.34);
    float alpha = innerFade * outerFade * (0.42 + 0.34 * turbulentMass + 0.24 * thread);

    float temperature = 1.0 - radius;
    vec3 gas = mix(vec3(0.22, 0.004, 0.006), vec3(0.91, 0.075, 0.012),
                   smoothstep(0.12, 0.78, temperature));
    gas = mix(gas, vec3(1.0, 0.37, 0.085), smoothstep(0.42, 0.86, temperature) * 0.72);
    gas = mix(gas, vec3(1.0, 0.86, 0.64),
              pow(temperature, 4.2) * (0.20 + thread * 0.65));
    gas *= 0.38 + broad * 0.78 + fine * 0.32 + thread * 0.35;

    // Aproximación de asimetría relativista: el lado que avanza es más brillante.
    float beaming = 0.56 + 0.78 * smoothstep(-0.65, 0.75, cos(angle + 0.45));
    gas *= beaming;
    gas += vec3(1.0, 0.76, 0.47) * thread * pow(temperature, 2.0) * 0.65;

    if (alpha < 0.012) discard;
    fragColor = vec4(gas, clamp(alpha, 0.0, 0.94));
}
