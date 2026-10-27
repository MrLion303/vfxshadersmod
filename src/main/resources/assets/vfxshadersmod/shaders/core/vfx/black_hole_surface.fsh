#version 150

uniform float BlackHoleTime;
uniform sampler2D BlackHoleReference;

in vec2 texCoord;
out vec4 fragColor;

float hash21(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float blackHoleNoise(vec2 p) {
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
    for (int i = 0; i < 4; i++) {
        value += blackHoleNoise(p) * amplitude;
        p = p * 2.03 + vec2(7.1, -3.8);
        amplitude *= 0.48;
    }
    return value / 0.9375;
}

void main() {
    vec2 p = texCoord * 2.0 - 1.0;
    float radius = length(vec2(p.x, p.y * 0.84));

    // Solo se dibuja el objeto astronómico: fuera del disco y del horizonte hay transparencia.
    if (radius > 0.49) {
        discard;
    }

    float angle = atan(p.y * 0.84, p.x);
    float time = BlackHoleTime * 0.0055;
    float logRadius = log(max(radius, 0.025));
    vec2 flow = vec2(
        angle * 2.4 + logRadius * 1.6 - time * 2.0,
        logRadius * 5.3 - angle * 0.72 + time * 0.82
    );

    float broad = fbm2(flow * 1.25);
    float detail = fbm2(flow * 3.4 + vec2(time * 0.45, -time * 0.7));
    float filaments = fbm2(flow * 6.2 + vec2(detail * 2.7, -broad * 2.0));
    float edgeWarp = (broad - 0.5) * 0.018 + (detail - 0.5) * 0.009;

    // Plasma caliente junto al horizonte, con filamentos y turbulencia asimétrica.
    float innerEdge = smoothstep(0.247 + edgeWarp, 0.270 + edgeWarp, radius);
    float outerEdge = 1.0 - smoothstep(0.380 + edgeWarp, 0.455 + edgeWarp, radius);
    float clumps = smoothstep(0.30, 0.69, broad * 0.62 + detail * 0.38);
    float threads = pow(max(0.0, sin(angle * 21.0 + filaments * 9.0 - time * 2.9)), 8.0);
    vec2 referenceUv = vec2(fract(angle / 6.28318530718),
            clamp((radius - 0.245) / 0.210, 0.0, 1.0));
    vec4 referenceDisk = texture(BlackHoleReference, referenceUv);
    float referenceDensity = smoothstep(0.035, 0.42, referenceDisk.a);
    float proceduralDensity = innerEdge * outerEdge * (0.54 + clumps * 0.46)
                            * (0.74 + threads * 0.26);
    float referenceMask = referenceDensity * smoothstep(0.242, 0.263, radius)
                        * (1.0 - smoothstep(0.43, 0.47, radius));
    float diskDensity = max(proceduralDensity * 0.38, referenceMask);

    float temperature = 1.0 - smoothstep(0.17, 0.445, radius);
    vec3 gasColor = mix(vec3(0.24, 0.006, 0.004), vec3(0.92, 0.085, 0.012),
                        smoothstep(0.10, 0.78, temperature));
    gasColor = mix(gasColor, vec3(1.0, 0.37, 0.075), pow(temperature, 2.1) * 0.82);
    gasColor = mix(gasColor, vec3(1.0, 0.83, 0.57),
                   pow(temperature, 4.6) * smoothstep(0.40, 0.77, filaments) * 0.82);
    gasColor *= 0.62 + broad * 0.78 + max(detail - 0.54, 0.0) * 0.52;
    gasColor = mix(gasColor, referenceDisk.rgb * 1.30,
            smoothstep(0.08, 0.40, referenceDisk.a) * referenceMask * 0.92);

    // El plasma se ve más luminoso en el lado que avanza hacia el observador.
    float beaming = 0.58 + 0.70 * smoothstep(-0.72, 0.76, cos(angle + 0.48));
    gasColor *= beaming;

    // Horizonte negro y anillo de fotones estrecho: sin aureola externa gigante.
    float horizonNoise = fbm2(p * 19.0 + vec2(time * 0.13, -time * 0.08));
    float horizonRadius = length(vec2(p.x, p.y * 0.95));
    float horizonEdge = 0.245 + (horizonNoise - 0.5) * 0.003;
    float eventHorizon = 1.0 - smoothstep(horizonEdge, horizonEdge + 0.006, horizonRadius);
    float photonRadius = 0.255 + (horizonNoise - 0.5) * 0.003;
    float photonRing = exp(-pow((horizonRadius - photonRadius) / 0.0048, 2.0));
    float innerGlow = exp(-pow((horizonRadius - 0.270) / 0.015, 2.0));

    vec3 color = gasColor * diskDensity;
    color += vec3(1.0, 0.86, 0.65) * photonRing * 1.55;
    color += vec3(1.0, 0.28, 0.045) * innerGlow * 0.34;
    color = mix(color, vec3(0.00005, 0.00004, 0.00008), eventHorizon);

    float alpha = max(diskDensity * 0.97, eventHorizon);
    alpha = max(alpha, max(photonRing * 0.99, innerGlow * 0.42));
    if (alpha < 0.012) {
        discard;
    }

    fragColor = vec4(max(color, vec3(0.0)), clamp(alpha, 0.0, 1.0));
}
