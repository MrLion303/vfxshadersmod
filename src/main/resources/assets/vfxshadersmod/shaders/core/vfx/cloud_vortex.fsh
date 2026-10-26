#version 150

uniform float VortexTime;

in vec2 texCoord;
out vec4 fragColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}
float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), u.x),
               mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), u.x), u.y);
}
float fbm(vec2 p) {
    float value = 0.0;
    float amplitude = 0.5;
    for (int i = 0; i < 6; i++) {
        value += noise(p) * amplitude;
        p = p * 2.02 + vec2(5.2, 3.9);
        amplitude *= 0.5;
    }
    return value;
}
void main() {
    vec2 p = (texCoord * 2.0 - 1.0) * 1.18;
    float r = length(p);
    float time = VortexTime * 0.012;
    // Distorsión de dominio: las nubes se estiran y se rompen en bandas de flujo.
    // No se dibujan anillos concéntricos regulares.
    vec2 warp = vec2(
        fbm(p * 2.4 + vec2(time * 0.7, -time * 0.4)),
        fbm(p * 2.4 + vec2(8.3, -3.6) + vec2(-time * 0.5, time * 0.35))
    );
    vec2 warped = p + (warp - 0.5) * 0.42;
    float warpedAngle = atan(warped.y, warped.x);
    float warpedRadius = length(warped);
    float twist = warpedAngle + warpedRadius * 3.9 - time;
    vec2 spiral = vec2(cos(twist), sin(twist)) * warpedRadius;
    float broad = fbm(spiral * 3.2 + warp * 1.4);
    float medium = fbm(spiral * 6.8 + vec2(2.7, -1.8) - time * 0.55);
    float fine = fbm(warped * 17.0 + vec2(-time * 0.8, time * 0.6));
    float wisps = fbm(warped * 31.0 + vec2(4.2, 9.1) + time * 0.25);
    float density = broad * 0.44 + medium * 0.31 + fine * 0.17 + wisps * 0.08;
    float cloudField = density + (fine - 0.5) * 0.23 + (wisps - 0.5) * 0.10;
    float cloudMask = smoothstep(0.43, 0.62, cloudField);
    float edgeNoise = fbm(warped * 5.0 + vec2(2.4, time * 0.18));
    float outerEdge = 1.0 - smoothstep(0.78 + (edgeNoise - 0.5) * 0.18, 1.12, r);
    float centralEye = smoothstep(0.045, 0.22, r);
    float filaments = smoothstep(0.54, 0.78, fine * 0.65 + wisps * 0.35);
    float clouds = cloudMask * outerEdge * centralEye;
    vec3 shadowRed = vec3(0.075, 0.009, 0.012);
    vec3 deepRed = vec3(0.24, 0.018, 0.018);
    vec3 burntOrange = vec3(0.72, 0.095, 0.025);
    vec3 hotOrange = vec3(1.0, 0.31, 0.055);
    vec3 paleHot = vec3(1.0, 0.72, 0.40);
    vec3 cloudColor = mix(shadowRed, deepRed, smoothstep(0.30, 0.58, density));
    cloudColor = mix(cloudColor, burntOrange, smoothstep(0.47, 0.68, medium));
    cloudColor = mix(cloudColor, hotOrange, filaments * smoothstep(0.48, 0.75, density) * 0.72);
    float core = exp(-r * 18.0);
    float innerGlow = exp(-r * 4.7) * 0.24;
    float hotRim = exp(-pow((r - 0.19 - (broad - 0.5) * 0.055) / 0.075, 2.0));
    vec3 color = cloudColor + vec3(1.0, 0.28, 0.06) * innerGlow;
    color += paleHot * hotRim * 0.42;
    color += vec3(1.0, 0.94, 0.78) * core * 2.15;
    float cloudAlpha = clouds * (0.38 + medium * 0.42);
    float alpha = cloudAlpha + innerGlow * 0.26 + core * 0.90 + hotRim * 0.10;
    alpha *= outerEdge;
    if (alpha < 0.012) discard;
    fragColor = vec4(color, clamp(alpha, 0.0, 0.94));
}
