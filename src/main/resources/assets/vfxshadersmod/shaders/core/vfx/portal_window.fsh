#version 150

uniform sampler2D DiffuseSampler;
uniform float UseTexture;
uniform float PortalTime;

in vec2 texCoord;
out vec4 fragColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), u.x),
               mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), u.x), u.y);
}

vec3 proceduralGalaxy(vec2 uv) {
    vec2 p = uv * 2.0 - 1.0;
    float radius = length(p);
    float angle = atan(p.y, p.x);
    float arms = 0.5 + 0.5 * sin(angle * 5.0 + radius * 24.0 - PortalTime * 0.035);
    float clouds = noise(p * 7.0 + vec2(PortalTime * 0.004, -PortalTime * 0.002));
    float wisps = noise(p * 15.0 + vec2(2.7, -1.4));
    vec3 deep = vec3(0.018, 0.012, 0.085);
    vec3 violet = vec3(0.20, 0.10, 0.48);
    vec3 cyan = vec3(0.10, 0.42, 0.74);
    vec3 color = mix(deep, violet, smoothstep(0.18, 0.82, clouds));
    color = mix(color, cyan, smoothstep(0.55, 0.92, arms) * (1.0 - radius) * 0.48);
    color += vec3(0.62, 0.35, 0.94) * wisps * (1.0 - smoothstep(0.45, 1.1, radius)) * 0.22;
    float core = exp(-radius * 8.0);
    color += vec3(0.45, 0.72, 1.0) * core * 0.78;
    float stars = step(0.993, hash(floor(uv * 360.0)));
    color += vec3(0.72, 0.88, 1.0) * stars * 1.6;
    return color;
}

void main() {
    vec2 p = texCoord * 2.0 - 1.0;
    float radius = length(p);
    if (radius > 1.0) {
        discard;
    }

    vec3 galaxy = proceduralGalaxy(texCoord);
    if (UseTexture > 0.5) {
        galaxy = texture(DiffuseSampler, texCoord).rgb;
    }
    float edge = 1.0 - smoothstep(0.92, 1.0, radius);
    float glow = 0.14 * (1.0 - smoothstep(0.35, 1.0, radius));
    galaxy += vec3(0.18, 0.52, 1.0) * glow;
    fragColor = vec4(galaxy, 0.98 * edge);
}
