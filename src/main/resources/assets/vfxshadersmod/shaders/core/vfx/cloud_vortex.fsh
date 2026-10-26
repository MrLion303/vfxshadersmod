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
    for (int i = 0; i < 5; i++) {
        value += noise(p) * amplitude;
        p = p * 2.03 + vec2(7.1, 3.7);
        amplitude *= 0.5;
    }
    return value;
}

void main() {
    vec2 p = texCoord * 2.0 - 1.0;
    float r = length(p);
    float angle = atan(p.y, p.x);
    float time = VortexTime * 0.018;

    // El campo de ruido se retuerce alrededor del centro para simular nubes arrastradas por un remolino.
    float twist = angle + r * 8.2 - time;
    vec2 flow = vec2(cos(twist), sin(twist)) * r;
    float lowCloud = fbm(flow * 7.0 + vec2(VortexTime * 0.005, -VortexTime * 0.003));
    float highCloud = fbm(flow * 15.0 + vec2(4.3, -2.1) - VortexTime * 0.008);
    float detail = fbm(flow * 24.0 + vec2(-VortexTime * 0.011, 8.4));
    float density = lowCloud * 0.48 + highCloud * 0.34 + detail * 0.18;
    float cloudMask = smoothstep(0.32, 0.67, density);
    float raggedEdge = 1.0 - smoothstep(0.78 + (density - 0.5) * 0.12, 1.03, r);
    float spiralGaps = smoothstep(0.22, 0.58, lowCloud * 0.72 + highCloud * 0.28);
    float clouds = cloudMask * raggedEdge * spiralGaps;

    vec3 deepRed = vec3(0.30, 0.018, 0.012);
    vec3 burntOrange = vec3(0.92, 0.115, 0.025);
    vec3 hotOrange = vec3(1.0, 0.35, 0.065);
    vec3 cloudColor = mix(deepRed, burntOrange, smoothstep(0.28, 0.76, density));
    cloudColor = mix(cloudColor, hotOrange, smoothstep(0.68, 0.9, detail) * 0.5);

    float core = exp(-r * 15.0);
    float innerGlow = exp(-r * 5.0) * 0.34;
    vec3 color = cloudColor + vec3(1.0, 0.55, 0.18) * innerGlow;
    color += vec3(1.0, 0.92, 0.68) * core * 2.6;

    float alpha = clouds * 0.88 + innerGlow * 0.30 + core * 0.92;
    alpha *= 1.0 - smoothstep(0.93, 1.02, r);
    if (alpha < 0.015) {
        discard;
    }
    fragColor = vec4(color, clamp(alpha, 0.0, 0.96));
}
