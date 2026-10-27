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
    float amplitude = 0.5;
    for (int i = 0; i < 4; i++) {
        value += noise2(p) * amplitude;
        p = p * 2.02 + vec2(7.1, -3.8);
        amplitude *= 0.5;
    }
    return value / 0.9375;
}

void main() {
    vec2 p = texCoord * 2.0 - 1.0;
    float radius = length(p);
    if (radius > 1.0) {
        discard;
    }

    float angle = atan(p.y, p.x);
    float time = PortalTime * 0.010;
    float broad = fbm2(p * 4.2 + vec2(time * 0.34, -time * 0.20));
    float detail = fbm2(p * 11.0 + vec2(-time * 0.63, time * 0.47));
    float warp = fbm2(p * 6.0 + vec2(3.4, -2.1) - time * 0.24) - 0.5;

    // El panorama se curva hacia el centro: deja de ser una imagen estática dentro de un aro.
    float swirlAngle = angle + (1.0 - radius) * 1.85 + warp * 0.72 - time * 0.82;
    float panoramaU = fract(swirlAngle / 6.28318530718 + 0.5);
    float panoramaV = clamp(0.5 - p.y * 0.47 + warp * 0.075
                            + sin(angle * 3.0 - time) * 0.018, 0.002, 0.998);
    vec3 panorama = texture(PortalSampler, vec2(panoramaU, panoramaV)).rgb;
    vec3 space = panorama * vec3(0.70, 0.73, 0.98);
    space = mix(space, vec3(0.012, 0.006, 0.035), 0.28);
    space = mix(space, vec3(0.16, 0.025, 0.24),
                smoothstep(0.36, 0.77, broad) * 0.48);
    space += vec3(0.16, 0.035, 0.30) * detail
           * (1.0 - smoothstep(0.12, 0.90, radius)) * 0.70;
    space += vec3(0.36, 0.055, 0.045) * pow(max(0.0, 1.0 - radius), 2.0) * 0.48;

    // Borde de plasma rugoso, irregular y luminoso, con tonos violetas y naranja solar.
    float edgeNoise = fbm2(vec2(angle * 3.7 + time * 0.42,
                                angle * 1.45 - time * 0.19));
    float ringRadius = 0.79 + (edgeNoise - 0.5) * 0.16
                     + sin(angle * 17.0 - time * 1.4) * 0.012;
    float flameNoise = fbm2(vec2(angle * 7.8 + time * 0.32,
                                 radius * 19.0 - time * 0.28));
    float flameWidth = 0.045 + flameNoise * 0.062;
    float ringDistance = abs(radius - ringRadius);
    float fire = 1.0 - smoothstep(flameWidth * 0.34, flameWidth, ringDistance);
    float filaments = pow(max(0.0, sin(angle * 29.0 + flameNoise * 8.5 - time * 1.2)), 7.0);
    float hotRim = fire * (0.58 + flameNoise * 0.58 + filaments * 0.42);

    vec3 rim = mix(vec3(0.16, 0.025, 0.46), vec3(1.0, 0.12, 0.018),
                   smoothstep(0.24, 0.82, flameNoise));
    rim = mix(rim, vec3(1.0, 0.72, 0.36), smoothstep(0.62, 0.96, hotRim) * 0.86);
    rim = mix(rim, vec3(0.92, 0.32, 1.0), filaments * 0.20);
    space = mix(space, rim * (1.10 + flameNoise * 0.95), clamp(hotRim, 0.0, 1.0));
    space += vec3(0.42, 0.09, 0.025) * fire * 0.30;

    // Núcleo de energía púrpura. Ya no se dibujan la antigua columna azul ni naves planas.
    float core = exp(-radius * 5.2) * (0.6 + broad * 0.4);
    space += vec3(0.34, 0.06, 0.48) * core * 0.75;
    space = mix(space, vec3(1.0, 0.55, 0.24),
                exp(-pow((radius - 0.20) / 0.07, 2.0)) * 0.22);

    float edgeFade = 1.0 - smoothstep(0.975, 1.0, radius);
    fragColor = vec4(max(space, vec3(0.0)), 0.985 * edgeFade);
}
