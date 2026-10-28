#version 150

uniform sampler2D PortalSampler;
uniform float PortalTime;

in vec2 texCoord;
out vec4 fragColor;

float hash21(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float portalNoise2(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash21(i);
    float b = hash21(i + vec2(1.0, 0.0));
    float c = hash21(i + vec2(0.0, 1.0));
    float d = hash21(i + vec2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

float portalFbm(vec2 p) {
    float value = 0.0;
    float amplitude = 0.54;
    for (int i = 0; i < 4; i++) {
        value += portalNoise2(p) * amplitude;
        p = p * 2.03 + vec2(7.1, -3.8);
        amplitude *= 0.48;
    }
    return value / 0.928;
}

void main() {
    vec2 p = texCoord * 2.0 - 1.0;
    float radius = length(p);
    if (radius >= 1.0) {
        discard;
    }

    float time = PortalTime * 0.012;
    float angle = atan(p.y, p.x);
    float logRadius = log(max(radius, 0.035));
    // Corrientes espirales de energía dan profundidad incluso si la textura de
    // referencia tiene un fondo negro o no termina de cargarse.
    vec2 flow = vec2(
        angle * 1.9 + logRadius * 2.2 - time,
        logRadius * 4.4 - angle * 0.72 + time * 0.34
    );
    float broad = portalFbm(flow * 1.35);
    float detail = portalFbm(flow * 3.2 + vec2(4.7 + time * 0.18, -3.8 - time * 0.23));
    float filaments = pow(max(0.0, sin(angle * 19.0 + detail * 8.0 - time * 1.6)), 5.0);
    float nebulaDensity = smoothstep(0.28, 0.75, broad * 0.72 + detail * 0.38 + filaments * 0.12);

    vec3 nebula = mix(vec3(0.006, 0.010, 0.035),
                      vec3(0.025, 0.20, 0.48), smoothstep(0.30, 0.72, broad));
    nebula = mix(nebula, vec3(0.30, 0.045, 0.46),
                 smoothstep(0.40, 0.78, detail) * 0.62);
    nebula = mix(nebula, vec3(0.06, 0.64, 0.82),
                 smoothstep(0.52, 0.83, broad + filaments * 0.24) * 0.78);
    nebula *= 0.66 + nebulaDensity * 1.02;
    float coreGlow = exp(-radius * 4.4) * (0.30 + detail * 0.72);
    nebula += vec3(0.12, 0.32, 0.82) * coreGlow;

    vec2 starGrid = texCoord * vec2(96.0, 64.0);
    vec2 starCell = floor(starGrid);
    vec2 starLocal = fract(starGrid) - vec2(0.5);
    float starSeed = hash21(starCell);
    float starShape = 1.0 - smoothstep(0.015, 0.12, length(starLocal));
    float stars = step(0.993, starSeed) * starShape
                * (0.62 + 0.38 * sin(PortalTime * 0.09 + starSeed * 37.0));

    vec3 sampled = texture(PortalSampler, vec2(texCoord.x, 1.0 - texCoord.y)).rgb;
    vec3 exposedImage = pow(max(sampled, vec3(0.0)), vec3(0.78)) * 1.22;
    float imageLuma = dot(exposedImage, vec3(0.2126, 0.7152, 0.0722));
    float imageWeight = mix(0.42, 0.68, smoothstep(0.015, 0.30, imageLuma));
    vec3 color = mix(nebula, exposedImage, imageWeight);
    color += vec3(0.62, 0.82, 1.0) * stars * 1.55;
    color += vec3(0.015, 0.08, 0.25) * filaments * (0.35 + broad * 0.75);

    float edgeFade = 1.0 - smoothstep(0.965, 1.0, radius);
    fragColor = vec4(max(color, vec3(0.0)), edgeFade);
}
