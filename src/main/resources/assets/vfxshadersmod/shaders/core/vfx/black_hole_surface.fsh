#version 150

uniform float BlackHoleTime;

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
        p = p * 2.03 + vec2(5.7, 9.2);
        amplitude *= 0.5;
    }
    return value / 0.9375;
}

float starLayer(vec2 p, float scale, float chance, float size) {
    vec2 grid = p * scale;
    vec2 cell = floor(grid);
    vec2 local = fract(grid) - 0.5;
    float seed = hash21(cell);
    vec2 offset = vec2(hash21(cell + 19.17), hash21(cell + 37.71)) - 0.5;
    float distanceToStar = length(local - offset * 0.68);
    float point = 1.0 - smoothstep(size, size * 2.25, distanceToStar);
    float selected = smoothstep(chance, chance + 0.08, seed);
    float brightness = 0.32 + 1.05 * hash21(cell + 71.3);
    return point * selected * brightness;
}

void main() {
    vec2 p = texCoord * 2.0 - 1.0;
    float time = BlackHoleTime * 0.002;

    // Approximate gravitational deflection of the procedurally generated star field.
    vec2 metric = vec2(p.x, p.y * 0.49);
    float distanceToHole = length(metric);
    vec2 radial = metric / max(distanceToHole, 0.0001);
    float deflection = 0.12 / (distanceToHole + 0.055);
    vec2 lensedMetric = radial * (distanceToHole + deflection * 0.10);
    vec2 lensed = vec2(lensedMetric.x, lensedMetric.y / 0.49);
    lensed *= 1.0 + min(2.8, deflection * 0.55);

    float nebula = fbm2(lensed * 3.1 + vec2(time * 0.35, -time * 0.18));
    float nebulaFine = fbm2(lensed * 7.0 + vec2(3.7, 1.4) - time * 0.24);
    vec3 background = mix(vec3(0.002, 0.004, 0.012),
                          vec3(0.026, 0.045, 0.105),
                          smoothstep(0.20, 0.76, nebula));
    background = mix(background, vec3(0.075, 0.025, 0.09),
                     smoothstep(0.56, 0.78, nebulaFine) * 0.42);

    float stars = starLayer(lensed, 22.0, 0.77, 0.020)
            + starLayer(lensed + vec2(9.3, 1.7), 39.0, 0.86, 0.017) * 0.72
            + starLayer(lensed + vec2(-2.4, 7.1), 61.0, 0.93, 0.012) * 0.56;
    vec3 color = background + vec3(0.67, 0.76, 1.0) * stars;
    float alpha = 0.16 + nebula * 0.10 + clamp(stars * 0.84, 0.0, 0.9);

    // Accretion gas: domain-warped turbulent flow and irregular filaments, computed per pixel.
    float angle = atan(p.y * 3.2, p.x);
    float diskRadius = length(vec2(p.x, p.y * 3.2));
    float logRadius = log(max(diskRadius, 0.035));
    vec2 flow = vec2(angle * 2.2 + logRadius * 1.7 - time * 2.0,
                     logRadius * 5.4 - angle * 0.62 + time * 0.72);
    vec2 warp = vec2(fbm2(flow * 1.25), fbm2(flow * 1.25 + vec2(7.1, -4.6))) - 0.5;
    float turbulent = fbm2(flow * 2.6 + warp * 2.3);
    float filaments = fbm2(flow * 5.7 + vec2(warp.y, -warp.x) * 3.4);
    float eddies = fbm2(flow * 9.3 + warp * 4.8);

    float noisyRadius = diskRadius + (turbulent - 0.5) * 0.070
            + (filaments - 0.5) * 0.023;
    float innerEdge = smoothstep(0.155, 0.235, noisyRadius);
    float outerEdge = 1.0 - smoothstep(0.63 + (turbulent - 0.5) * 0.11,
                                        0.86 + (filaments - 0.5) * 0.07, noisyRadius);
    float clumps = smoothstep(0.285, 0.665,
            turbulent * 0.59 + filaments * 0.31 + eddies * 0.10);
    float fineThreads = 0.62 + 0.38 * smoothstep(0.30, 0.82, eddies);
    float diskDensity = innerEdge * outerEdge * clumps * fineThreads;

    float temperature = 1.0 - smoothstep(0.20, 0.79, noisyRadius);
    vec3 gasColor = mix(vec3(0.48, 0.022, 0.009), vec3(1.0, 0.22, 0.028), temperature);
    gasColor = mix(gasColor, vec3(1.0, 0.72, 0.36), pow(temperature, 3.5) * 0.82);
    gasColor = mix(gasColor, vec3(1.0, 0.96, 0.82),
                   pow(temperature, 7.0) * smoothstep(0.48, 0.78, filaments) * 0.80);

    float approachingSide = 0.48 + 1.08 * smoothstep(-0.82, 0.82, cos(angle + 0.42));
    float emission = clamp(diskDensity * approachingSide * 1.24, 0.0, 1.0);
    color = mix(color, gasColor * (1.05 + turbulent * 0.42), emission);
    color += gasColor * diskDensity * pow(max(0.0, filaments - 0.47), 2.0) * 0.64;
    alpha = max(alpha, diskDensity * 0.94);

    // Photon ring and event horizon are shade functions, not polygon rings or a sphere mesh.
    float horizonNoise = fbm2(p * 19.0 + vec2(time * 0.2, -time * 0.11));
    float horizonRadius = length(vec2(p.x, p.y * 0.49));
    float horizonEdge = 0.158 + (horizonNoise - 0.5) * 0.006;
    float eventHorizon = 1.0 - smoothstep(horizonEdge, horizonEdge + 0.023, horizonRadius);
    float photonNoise = fbm2(p * 27.0 + vec2(3.0, time * 0.09));
    float photonRadius = 0.194 + (photonNoise - 0.5) * 0.009;
    float photonRing = exp(-pow((horizonRadius - photonRadius) / 0.015, 2.0));
    float glow = exp(-pow((horizonRadius - 0.252) / 0.044, 2.0));

    color += vec3(1.0, 0.81, 0.52) * photonRing * (0.64 + turbulent * 0.58);
    color += vec3(1.0, 0.29, 0.075) * glow * 0.24;
    color = mix(color, vec3(0.0004, 0.0005, 0.0012), eventHorizon);
    alpha = max(alpha, max(eventHorizon, max(photonRing * 0.92, glow * 0.45)));

    // Soft, noisy silhouette fades into the Minecraft sky instead of revealing a rectangle.
    float edgeNoise = fbm2(p * 2.6 + vec2(2.1, -time * 0.11));
    float silhouette = 1.0 - smoothstep(0.94 + (edgeNoise - 0.5) * 0.12,
                                         1.29 + (edgeNoise - 0.5) * 0.08, length(p));
    alpha *= silhouette;
    if (alpha < 0.012) {
        discard;
    }
    fragColor = vec4(max(color, vec3(0.0)), clamp(alpha, 0.0, 0.97));
}
