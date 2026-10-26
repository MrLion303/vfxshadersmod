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
    float horizonRadius = clamp(hole.z, 0.001, 0.25);
    if (d < 0.00001) return uv;

    float strength = clamp(hole.w, 0.0, 1.45);
    float reach = horizonRadius * 5.2;
    float fade = 1.0 - smoothstep(horizonRadius * 1.05, reach, d);

    // Deflection grows toward the hole and folds at the approximate Einstein radius.
    float einsteinRadius = horizonRadius * 1.55;
    float deflection = (einsteinRadius * einsteinRadius)
            / max(d, horizonRadius * 0.24);
    deflection = min(deflection, horizonRadius * 2.8);
    float mappedDistance = d - deflection * strength * fade;

    vec2 direction = metric / d;
    vec2 mappedMetric = direction * mappedDistance;
    return hole.xy + vec2(mappedMetric.x / aspect, mappedMetric.y);
}

float eventHorizonMask(vec2 screenUv, vec4 hole, float aspect) {
    if (hole.z <= 0.0) return 0.0;
    vec2 delta = screenUv - hole.xy;
    float d = length(vec2(delta.x * aspect, delta.y));
    float radius = clamp(hole.z, 0.001, 0.25);
    return 1.0 - smoothstep(radius * 0.87, radius * 1.08, d);
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

    // The procedural surface already generates the accretion disk and photon ring.
    // This final pass only lenses the actual rendered scene and keeps the horizon dark.
    float horizon = max(
        max(eventHorizonMask(texCoord, Hole0, aspect), eventHorizonMask(texCoord, Hole1, aspect)),
        max(eventHorizonMask(texCoord, Hole2, aspect), eventHorizonMask(texCoord, Hole3, aspect))
    );
    horizon = max(horizon, max(
        max(eventHorizonMask(texCoord, Hole4, aspect), eventHorizonMask(texCoord, Hole5, aspect)),
        max(eventHorizonMask(texCoord, Hole6, aspect), eventHorizonMask(texCoord, Hole7, aspect))
    ));

    scene = mix(scene, vec3(0.0002, 0.0003, 0.0007), horizon);
    fragColor = vec4(scene, 1.0);
}
