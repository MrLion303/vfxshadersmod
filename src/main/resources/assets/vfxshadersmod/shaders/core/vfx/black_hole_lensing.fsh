#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;
uniform vec2 ScreenSize;
uniform float RainbowSkyActive;
uniform vec4 RainbowSkyColor;
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

    // Curvatura fuerte y amplia: la influencia alcanza varias veces el radio aparente.
    float falloff = exp(-(distanceToCenter * distanceToCenter) / (radius * radius * 42.0));
    float offset = (radius * radius * hole.w) /
            (distanceToCenter + radius * 0.06 + 0.0005) * falloff;

    if (distanceToCenter > 0.00001) {
        metricDelta /= distanceToCenter;
        uv += vec2(metricDelta.x / aspect, metricDelta.y) * offset;
    }
    return uv;
}

void addGargantuaLook(inout vec3 color, vec2 screenUv, vec4 hole, float aspect) {
    if (hole.z <= 0.0) {
        return;
    }

    vec2 delta = screenUv - hole.xy;
    vec2 metricDelta = vec2(delta.x * aspect, delta.y);
    float radius = max(hole.z, 0.0012);
    float distanceToCenter = length(metricDelta);
    float normalizedDistance = distanceToCenter / radius;
    float intensity = clamp(hole.w / 1.55, 0.0, 1.5);

    // Conserva la sombra central negra incluso cuando el fondo se refracta.
    float shadow = 1.0 - smoothstep(0.84, 1.06, normalizedDistance);
    color *= 1.0 - shadow;

    // Disco de acreción horizontal blanco-dorado y arcos que envuelven la sombra.
    float bandWidth = max(radius * 0.105, 0.0015);
    float horizontalDistance = abs(metricDelta.x);
    float diskBand = exp(-pow(metricDelta.y / bandWidth, 2.0));
    diskBand *= smoothstep(radius * 0.76, radius * 1.10, horizontalDistance);
    diskBand *= 1.0 - smoothstep(radius * 2.55, radius * 3.85, horizontalDistance);

    float innerHalo = exp(-pow((distanceToCenter - radius * 1.12) /
            max(radius * 0.105, 0.0015), 2.0));
    float outerHalo = exp(-pow((distanceToCenter - radius * 1.47) /
            max(radius * 0.19, 0.002), 2.0));

    color += vec3(1.0, 0.86, 0.63) * diskBand * (0.78 * intensity);
    color += vec3(1.0, 0.96, 0.84) * innerHalo * (0.76 * intensity);
    color += vec3(1.0, 0.68, 0.36) * outerHalo * (0.26 * intensity);
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

    if (RainbowSkyActive > 0.5) {
        float depth = texture(DepthSampler, texCoord).r;
        float pureSky = smoothstep(0.9995, 0.99999, depth);
        scene.rgb = mix(scene.rgb, RainbowSkyColor.rgb, pureSky);

        float maximumChannel = max(scene.r, max(scene.g, scene.b));
        float minimumChannel = min(scene.r, min(scene.g, scene.b));
        float saturation = (maximumChannel - minimumChannel) / max(maximumChannel, 0.001);
        float luminance = dot(scene.rgb, vec3(0.2126, 0.7152, 0.0722));

        // Las nubes son lejanas, claras y de baja saturación. La máscara las tiñe
        // sin cubrir su volumen ni recolorear los bloques cercanos.
        float farMask = smoothstep(0.990, 0.997, depth)
                * (1.0 - smoothstep(0.99995, 1.0, depth));
        float cloudMask = farMask
                * (1.0 - smoothstep(0.05, 0.25, saturation))
                * smoothstep(0.20, 0.65, luminance)
                * smoothstep(0.30, 0.60, texCoord.y);
        vec3 cloudColor = RainbowSkyColor.rgb * (0.48 + 0.62 * luminance);
        scene.rgb = mix(scene.rgb, cloudColor, clamp(cloudMask * 0.96, 0.0, 1.0));
    }

    fragColor = scene;
}
