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

// Screen-space lensing: map visible rays back to their source directions.
// The radial fold near the Einstein radius approximates a secondary image.
vec2 bendAroundHole(vec2 uv, vec4 hole, float aspect) {
    if (hole.z <= 0.0) return uv;

    vec2 delta = uv - hole.xy;
    vec2 metric = vec2(delta.x * aspect, delta.y);
    float d = length(metric);
    float horizonRadius = clamp(hole.z, 0.001, 0.46);
    if (d < 0.00001) return uv;

    float strength = clamp(hole.w, 0.0, 1.45);
    float reach = horizonRadius * 3.6;
    float fade = 1.0 - smoothstep(horizonRadius * 1.05, reach, d);

    // Deflection grows toward the hole and folds at the approximate Einstein radius.
    float einsteinRadius = horizonRadius * 1.55;
    float deflection = (einsteinRadius * einsteinRadius)
            / max(d, horizonRadius * 0.24);
    deflection = min(deflection, horizonRadius * 1.9);
    float mappedDistance = d - deflection * strength * fade;

    vec2 direction = metric / d;
    vec2 mappedMetric = direction * mappedDistance;
    return hole.xy + vec2(mappedMetric.x / aspect, mappedMetric.y);
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

    vec3 scene = texture(DiffuseSampler, clamp(uv, vec2(0.001), vec2(0.999))).rgb;

    // La sombra visible viene de la esfera 3D; este pase solo deforma la escena.
    fragColor = vec4(scene, 1.0);
}
