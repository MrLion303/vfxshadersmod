#version 150

uniform sampler2D PortalSampler;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec2 p = texCoord * 2.0 - 1.0;
    float radius = length(p);
    if (radius >= 1.0) {
        discard;
    }

    // El interior es únicamente panorama_overlay.png, sin nebulosas ni un fondo
    // espacial inventado por shader.
    vec2 panoramaUV = vec2(texCoord.x, 1.0 - texCoord.y);
    vec3 panorama = texture(PortalSampler, panoramaUV).rgb;
    float edgeFade = 1.0 - smoothstep(0.982, 1.0, radius);
    fragColor = vec4(panorama, edgeFade);
}
