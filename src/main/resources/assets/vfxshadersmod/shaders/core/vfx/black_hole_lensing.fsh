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
    if (hole.z <= 0.0) return uv;

    vec2 delta = uv - hole.xy;
    vec2 metric = vec2(delta.x * aspect, delta.y);
    float d = length(metric);
    float radius = clamp(hole.z, 0.001, 0.25);

    // La zona deformada queda ligada al tamaño aparente del agujero:
    // el alcance visible es de aproximadamente un radio a su alrededor.
    float reach = radius * 2.2;
    float falloff = 1.0 - smoothstep(radius * 0.18, reach, d);
    float strength = clamp(hole.w, 0.0, 1.4);
    float offset = radius * 0.18 * strength * falloff
            / (1.0 + d / max(radius, 0.001));

    if (d > 0.00001) {
        metric /= d;
        uv += vec2(metric.x / aspect, metric.y) * offset;
    }
    return uv;
}

void addGargantuaLook(inout vec3 color, vec2 screenUv, vec4 hole, float aspect) {
    if (hole.z <= 0.0) return;

    vec2 delta = screenUv - hole.xy;
    vec2 metric = vec2(delta.x * aspect, delta.y);
    float radius = clamp(hole.z, 0.001, 0.25);
    float d = length(metric);
    float normalizedDistance = d / radius;
    float intensity = clamp(hole.w / 1.4, 0.0, 1.0);

    // Solo el núcleo del agujero queda negro; nunca se oscurece la pantalla completa.
    float shadow = 1.0 - smoothstep(0.82, 1.02, normalizedDistance);
    color *= 1.0 - shadow;

    float bandWidth = max(radius * 0.10, 0.0012);
    float diskBand = exp(-pow(metric.y / bandWidth, 2.0));
    diskBand *= smoothstep(radius * 0.72, radius * 1.04, abs(metric.x));
    diskBand *= 1.0 - smoothstep(radius * 2.1, radius * 3.0, abs(metric.x));

    float innerHalo = exp(-pow((d - radius * 1.10) / max(radius * 0.10, 0.0012), 2.0));
    float outerHalo = exp(-pow((d - radius * 1.42) / max(radius * 0.17, 0.0015), 2.0));

    color += vec3(1.0, 0.86, 0.63) * diskBand * (0.72 * intensity);
    color += vec3(1.0, 0.96, 0.84) * innerHalo * (0.68 * intensity);
    color += vec3(1.0, 0.68, 0.36) * outerHalo * (0.22 * intensity);
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

    vec4 scene = texture(DiffuseSampler, clamp(uv, vec2(0.001), vec2(0.999)));
    addGargantuaLook(scene.rgb, texCoord, Hole0, aspect);
    addGargantuaLook(scene.rgb, texCoord, Hole1, aspect);
    addGargantuaLook(scene.rgb, texCoord, Hole2, aspect);
    addGargantuaLook(scene.rgb, texCoord, Hole3, aspect);
    addGargantuaLook(scene.rgb, texCoord, Hole4, aspect);
    addGargantuaLook(scene.rgb, texCoord, Hole5, aspect);
    addGargantuaLook(scene.rgb, texCoord, Hole6, aspect);
    addGargantuaLook(scene.rgb, texCoord, Hole7, aspect);

    fragColor = vec4(scene.rgb, 1.0);
}
