#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 ScreenSize;
uniform vec4 Hole0;
uniform vec4 Hole1;
uniform vec4 Hole2;
uniform vec4 Hole3;
uniform vec4 Hole4;
uniform vec4 Hole5;
uniform vec4 Hole6;
uniform vec4 Hole7;

in vec2 texCoord;
out vec4 fragColor;

vec2 bendAroundHole(vec2 uv, vec4 hole, float aspect) {
    if (hole.z <= 0.0) {
        return uv;
    }

    vec2 delta = uv - hole.xy;
    vec2 metricDelta = vec2(delta.x * aspect, delta.y);
    float distanceToCenter = length(metricDelta);
    float radius = max(hole.z, 0.0012);

    // La distorsión aumenta cerca del horizonte y cae suavemente con la distancia.
    float falloff = exp(-(distanceToCenter * distanceToCenter) / (radius * radius * 10.0));
    float offset = (radius * radius * hole.w) /
            (distanceToCenter + radius * 0.14 + 0.0005) * falloff;

    if (distanceToCenter > 0.00001) {
        metricDelta /= distanceToCenter;
        uv += vec2(metricDelta.x / aspect, metricDelta.y) * offset;
    }
    return uv;
}

void main() {
    float aspect = max(ScreenSize.x / max(ScreenSize.y, 1.0), 0.1);
    vec2 uv = texCoord;
    uv = bendAroundHole(uv, Hole0, aspect);
    uv = bendAroundHole(uv, Hole1, aspect);
    uv = bendAroundHole(uv, Hole2, aspect);
    uv = bendAroundHole(uv, Hole3, aspect);
    uv = bendAroundHole(uv, Hole4, aspect);
    uv = bendAroundHole(uv, Hole5, aspect);
    uv = bendAroundHole(uv, Hole6, aspect);
    uv = bendAroundHole(uv, Hole7, aspect);
    fragColor = texture(DiffuseSampler, clamp(uv, vec2(0.001), vec2(0.999)));
}
