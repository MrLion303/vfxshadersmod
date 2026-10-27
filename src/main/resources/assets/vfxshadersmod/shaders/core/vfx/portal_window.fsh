#version 150

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

float fbm2(vec2 p) {
    float value = 0.0;
    float amplitude = 0.5;
    for (int i = 0; i < 4; i++) {
        value += portalNoise2(p) * amplitude;
        p = p * 2.02 + vec2(7.1, -3.8);
        amplitude *= 0.5;
    }
    return value / 0.9375;
}

float saucer(vec2 p, vec2 center, vec2 size) {
    vec2 q = (p - center) / size;
    float body = 1.0 - smoothstep(0.78, 1.0, length(vec2(q.x, q.y * 2.6)));
    float dome = 1.0 - smoothstep(0.34, 0.48, length(vec2(q.x * 1.8, (q.y - 0.18) * 3.5)));
    return max(body, dome * step(0.0, q.y));
}

void main() {
    vec2 p = texCoord * 2.0 - 1.0;
    float radius = length(p);
    if (radius > 1.0) discard;

    float angle = atan(p.y, p.x);
    float time = PortalTime * 0.008;
    float broad = fbm2(p * 3.4 + vec2(time * 0.3, -time * 0.16));
    float clouds = fbm2(p * 8.0 + vec2(2.4, -3.2) - time * 0.22);
    float stars = step(0.994, hash21(floor(texCoord * 410.0)));
    float nebula = smoothstep(0.30, 0.80, broad * 0.65 + clouds * 0.35);

    vec3 space = mix(vec3(0.012, 0.018, 0.052), vec3(0.16, 0.045, 0.16), nebula);
    space = mix(space, vec3(0.27, 0.075, 0.12), smoothstep(0.48, 0.88, clouds) * 0.34);
    space += vec3(0.42, 0.26, 0.50) * clouds * (1.0 - smoothstep(0.4, 1.0, radius)) * 0.22;
    space += vec3(0.78, 0.86, 1.0) * stars * 1.35;

    // Un horizonte de destrucción: un anillo grueso, irregular y caliente, no un aro cian fino.
    float edgeNoise = fbm2(vec2(angle * 1.9, angle * 0.75) + vec2(time * 0.12, -time * 0.08));
    float ringRadius = 0.825 + (edgeNoise - 0.5) * 0.095
                     + sin(angle * 17.0 + time * 0.18) * 0.012;
    float flameNoise = fbm2(vec2(angle * 7.2 + time * 0.32, radius * 24.0 - time * 0.21));
    float flameHeight = 0.10 + flameNoise * 0.075;
    float ringDistance = abs(radius - ringRadius);
    float fire = 1.0 - smoothstep(flameHeight * 0.45, flameHeight, ringDistance);
    float innerEdge = 1.0 - smoothstep(0.0, 0.15, radius - (ringRadius - flameHeight * 0.5));
    float filaments = pow(max(0.0, sin(angle * 31.0 + flameNoise * 7.5 - time * 0.65)), 7.0);
    float hotRim = fire * (0.62 + flameNoise * 0.55 + filaments * 0.38);

    vec3 fireColor = mix(vec3(0.30, 0.012, 0.006), vec3(1.0, 0.16, 0.018),
                         smoothstep(0.12, 0.76, flameNoise));
    fireColor = mix(fireColor, vec3(1.0, 0.64, 0.28), smoothstep(0.55, 0.91, hotRim) * 0.84);
    fireColor = mix(fireColor, vec3(1.0, 0.92, 0.72), filaments * fire * 0.50);
    space = mix(space, fireColor * (1.2 + flameNoise * 0.75), clamp(hotRim, 0.0, 1.0));
    space += vec3(0.68, 0.16, 0.035) * fire * 0.34;

    // Columna central dentada que se extiende desde el borde inferior hacia la estructura orbital.
    float towerT = clamp((p.y + 1.0) / 1.78, 0.0, 1.0);
    float towerNoise = fbm2(vec2(p.y * 17.0, p.x * 23.0 + time * 0.03));
    float towerCenter = 0.035 * sin(p.y * 5.7 + time * 0.025) + (towerNoise - 0.5) * 0.055;
    float towerWidth = mix(0.105, 0.018, towerT) + (towerNoise - 0.5) * 0.035;
    float towerMask = step(-1.0, p.y) * (1.0 - smoothstep(0.71, 0.82, p.y))
                    * (1.0 - smoothstep(towerWidth, towerWidth + 0.016, abs(p.x - towerCenter)));
    vec3 tower = mix(vec3(0.009, 0.014, 0.026), vec3(0.075, 0.16, 0.22),
                     smoothstep(0.40, 0.78, towerNoise));
    float blueRim = 1.0 - smoothstep(0.005, 0.021, abs(p.x - towerCenter - towerWidth * 0.58));
    tower += vec3(0.08, 0.31, 0.54) * blueRim * 0.30;
    space = mix(space, tower, towerMask);

    // Siluetas pequeñas de estructuras flotantes dentro de la brecha.
    float ships = max(saucer(p, vec2(-0.50, 0.38), vec2(0.17, 0.06)),
                 max(saucer(p, vec2(0.48, 0.50), vec2(0.15, 0.055)),
                     saucer(p, vec2(0.42, 0.10), vec2(0.12, 0.045))));
    float shipWindows = step(0.84, hash21(floor((p + vec2(0.5, -0.4)) * 90.0)));
    vec3 shipColor = vec3(0.045, 0.025, 0.025) + vec3(0.42, 0.12, 0.025) * shipWindows;
    space = mix(space, shipColor, ships * 0.94);

    float edgeFade = 1.0 - smoothstep(0.975, 1.0, radius);
    float glow = exp(-abs(radius - ringRadius) * 18.0) * 0.26;
    space += vec3(1.0, 0.25, 0.055) * glow;
    fragColor = vec4(max(space, vec3(0.0)), 0.985 * edgeFade);
}
