#version 150

uniform vec3 CameraForward;
uniform vec3 CameraRight;
uniform vec3 CameraUp;
uniform vec3 VortexDirection;
uniform vec3 VortexRight;
uniform vec3 VortexUp;
uniform float TanHalfFov;
uniform float ScreenAspect;
uniform float VortexTime;
uniform sampler2D VortexReference;

in vec2 texCoord;
out vec4 fragColor;

const float VORTEX_DISTANCE = 224.0;
const float VORTEX_HALF_WIDTH = 90.0;
const float VORTEX_HALF_HEIGHT = 78.0;

float hash21(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float noise21(vec2 p) {
    vec2 cell = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash21(cell);
    float b = hash21(cell + vec2(1.0, 0.0));
    float c = hash21(cell + vec2(0.0, 1.0));
    float d = hash21(cell + vec2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

void main() {
    vec2 screen = texCoord * 2.0 - 1.0;
    vec3 rayDirection = normalize(
        CameraForward
        + CameraRight * (screen.x * ScreenAspect * TanHalfFov)
        + CameraUp * (screen.y * TanHalfFov)
    );

    // La textura se proyecta sobre un plano distante perpendicular al eje del vórtice.
    // El plano está en coordenadas de mundo relativas a la cámara, no pegado a la pantalla.
    float facing = dot(rayDirection, VortexDirection);
    if (facing <= 0.0001) discard;

    float distanceAlongRay = VORTEX_DISTANCE / facing;
    vec3 center = VortexDirection * VORTEX_DISTANCE;
    vec3 relative = rayDirection * distanceAlongRay - center;
    vec2 local = vec2(
        dot(relative, VortexRight) / VORTEX_HALF_WIDTH,
        dot(relative, VortexUp) / VORTEX_HALF_HEIGHT
    );
    if (abs(local.x) >= 1.0 || abs(local.y) >= 1.0) discard;

    float radius = length(local);
    float angle = atan(local.y, local.x);
    float time = VortexTime * 0.006;

    // Deformación sutil de las coordenadas: conserva el contorno de la referencia,
    // añadiendo un giro lento y turbulencia animada.
    float twist = (1.0 - smoothstep(0.0, 1.05, radius)) * 0.16
                + sin(radius * 19.0 - time * 3.8) * 0.012
                + (noise21(local * 7.0 + vec2(time, -time * 0.7)) - 0.5) * 0.045;
    vec2 warped = vec2(cos(angle + twist), sin(angle + twist)) * radius;
    warped += vec2(
        sin(local.y * 16.0 + time * 4.1),
        cos(local.x * 13.0 - time * 3.6)
    ) * 0.006;

    vec2 uv = vec2(0.5 + warped.x * 0.5, 0.5 - warped.y * 0.5);
    vec4 guide = texture(VortexReference, clamp(uv, vec2(0.001), vec2(0.999)));
    if (guide.a < 0.015) discard;

    // Segunda capa discreta para dar profundidad sin dibujar una esfera ni un halo circular.
    vec2 detailUv = vec2(0.5) + (uv - vec2(0.5)) * 1.035
                  + vec2(time * 0.002, -time * 0.0013);
    vec4 detail = texture(VortexReference, clamp(detailUv, vec2(0.001), vec2(0.999)));
    float turbulence = noise21(warped * 18.0 + vec2(time * 1.5, -time));
    vec3 color = mix(detail.rgb, guide.rgb, 0.78);
    color *= 0.86 + turbulence * 0.24;

    float hotCore = exp(-radius * radius * 25.0) * guide.a;
    color = mix(color, vec3(1.0, 0.39, 0.085), hotCore * 0.20);
    float alpha = clamp(max(guide.a, detail.a * 0.18) * (0.93 + turbulence * 0.10), 0.0, 0.98);
    fragColor = vec4(color, alpha);
}
