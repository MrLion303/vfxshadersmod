#version 150

uniform float BlackHoleTime;

in vec2 texCoord;
out vec4 fragColor;

float hash21(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float bhSurfaceNoise2(vec2 p) {
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
        value += bhSurfaceNoise2(p) * amplitude;
        p = p * 2.07 + vec2(5.7, 9.2);
        amplitude *= 0.48;
    }
    return value / 0.9196;
}

void main() {
    vec2 p = texCoord * 2.0 - 1.0;

    // Horizon elíptico y disco horizontal: fuera del agujero no se dibuja un cielo falso.
    float horizonRadius = length(vec2(p.x, p.y * 1.10));
    float diskRadius = length(vec2(p.x, p.y * 1.30));
    if (horizonRadius > 0.245 && diskRadius > 0.78) {
        discard;
    }

    float time = BlackHoleTime * 0.002;
    float angle = atan(p.y * 1.30, p.x);
    float logRadius = log(max(diskRadius, 0.035));
    vec2 flow = vec2(
        angle * 2.1 + logRadius * 1.25 - time * 1.85,
        logRadius * 4.7 - angle * 0.62 + time * 0.74
    );
    vec2 warp = vec2(
        fbm2(flow * 1.35),
        fbm2(flow * 1.35 + vec2(7.1, -4.6))
    ) - 0.5;
    float turbulence = fbm2(flow * 2.65 + warp * 2.2);
    float filaments = fbm2(flow * 5.5 + vec2(warp.y, -warp.x) * 3.2);
    float fineDetail = bhSurfaceNoise2(flow * 13.0 + vec2(time * 0.4, -time * 0.7));

    float innerEdge = smoothstep(0.162, 0.222, diskRadius + (turbulence - 0.5) * 0.018);
    float outerEdge = 1.0 - smoothstep(
        0.51 + (turbulence - 0.5) * 0.035,
        0.66 + (filaments - 0.5) * 0.035,
        diskRadius
    );
    float clumps = smoothstep(0.29, 0.69,
        turbulence * 0.57 + filaments * 0.33 + fineDetail * 0.10);
    float streamers = 0.62 + 0.38 * smoothstep(0.36, 0.82, filaments);
    float diskDensity = innerEdge * outerEdge * clumps * streamers;

    float temperature = 1.0 - smoothstep(0.19, 0.67, diskRadius);
    vec3 gasColor = mix(vec3(0.19, 0.004, 0.006), vec3(1.0, 0.17, 0.025), temperature);
    gasColor = mix(gasColor, vec3(1.0, 0.56, 0.22), pow(temperature, 2.7) * 0.82);
    gasColor = mix(gasColor, vec3(1.0, 0.91, 0.76),
        pow(temperature, 5.6) * smoothstep(0.43, 0.79, filaments) * 0.72);
    gasColor *= 0.64 + turbulence * 0.83 + max(fineDetail - 0.56, 0.0) * 0.55;

    // La región negra es el horizonte; el anillo de fotones es estrecho, no una gran aureola.
    float horizonNoise = fbm2(p * 17.0 + vec2(time * 0.13, -time * 0.09));
    float horizonEdge = 0.153 + (horizonNoise - 0.5) * 0.0045;
    float eventHorizon = 1.0 - smoothstep(horizonEdge, horizonEdge + 0.008, horizonRadius);
    float photonRadius = 0.164 + (horizonNoise - 0.5) * 0.005;
    float photonRing = exp(-pow((horizonRadius - photonRadius) / 0.007, 2.0));
    float innerRelativisticGlow = exp(-pow((horizonRadius - 0.177) / 0.018, 2.0));

    vec3 color = gasColor * diskDensity;
    color += vec3(1.0, 0.84, 0.62) * photonRing * 1.4;
    color += vec3(1.0, 0.31, 0.075) * innerRelativisticGlow * 0.42;
    color = mix(color, vec3(0.0001, 0.0001, 0.0002), eventHorizon);

    float alpha = max(diskDensity * 0.96, eventHorizon);
    alpha = max(alpha, max(photonRing * 0.98, innerRelativisticGlow * 0.54));
    if (alpha < 0.012) {
        discard;
    }
    fragColor = vec4(max(color, vec3(0.0)), clamp(alpha, 0.0, 1.0));
}
