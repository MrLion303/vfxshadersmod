#version 150
uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;
uniform vec2 ScreenSize;
uniform vec3 CameraPosition;
uniform vec3 CameraRight;
uniform vec3 CameraUp;
uniform vec3 CameraForward;
uniform vec3 WaveOrigin;
uniform vec4 WaveParams;
uniform mat4 ProjMat;
uniform mat4 InverseProjectionMat;
in vec2 texCoord;
out vec4 fragColor;

vec4 sceneAt(vec2 uv) {
    return texture(DiffuseSampler, clamp(uv, vec2(0.001), vec2(0.999)));
}

void main() {
    vec4 originalColor = sceneAt(texCoord);
    float depth = texture(DepthSampler, texCoord).r;
    if (depth >= 0.99998) {
        fragColor = originalColor;
        return;
    }

    vec4 clipPosition = vec4(texCoord * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec4 viewPosition4 = InverseProjectionMat * clipPosition;
    if (abs(viewPosition4.w) < 0.00001) {
        fragColor = originalColor;
        return;
    }
    vec3 viewPosition = viewPosition4.xyz / viewPosition4.w;
    vec3 worldPosition = CameraPosition
            + CameraRight * viewPosition.x
            + CameraUp * viewPosition.y
            - CameraForward * viewPosition.z;

    vec2 fromOrigin = worldPosition.xz - WaveOrigin.xz;
    float radialDistance = length(fromOrigin);
    float frontRadius = WaveParams.x;
    float maximumRadius = WaveParams.y;
    float amplitude = WaveParams.z;
    float energy = WaveParams.w;

    // Acota el efecto al terreno alrededor de la altura donde se disparó la onda.
    if (radialDistance > maximumRadius + 1.0
            || worldPosition.y < WaveOrigin.y - 3.5
            || worldPosition.y > WaveOrigin.y + 5.0) {
        fragColor = originalColor;
        return;
    }

    float distanceFromCrest = radialDistance - frontRadius;
    float envelope = exp(-abs(distanceFromCrest) * 0.26);
    float waveHeight = sin(distanceFromCrest * 1.36) * envelope * amplitude * energy * 1.18;
    vec2 radialDirection = fromOrigin / max(radialDistance, 0.001);
    vec3 worldDisplacement = vec3(
            radialDirection.x * waveHeight * 0.32,
            waveHeight,
            radialDirection.y * waveHeight * 0.32
    );

    vec3 displacementInView = vec3(
            dot(worldDisplacement, CameraRight),
            dot(worldDisplacement, CameraUp),
            -dot(worldDisplacement, CameraForward)
    );
    vec4 displacedClip = ProjMat * vec4(viewPosition + displacementInView, 1.0);
    if (displacedClip.w <= 0.0001) {
        fragColor = originalColor;
        return;
    }

    vec2 displacedUV = displacedClip.xy / displacedClip.w * 0.5 + 0.5;
    vec2 uvDelta = displacedUV - texCoord;
    float radialMask = 1.0 - smoothstep(maximumRadius - 2.0, maximumRadius + 1.0, radialDistance);
    vec2 sourceUV = texCoord - uvDelta * radialMask * 1.18;

    // Vibración breve que acentúa las juntas y las siluetas de los bloques en la cresta.
    float ripple = sin(distanceFromCrest * 2.8) * envelope * 8.0 * energy;
    sourceUV += normalize(uvDelta + vec2(0.000001)) * ripple / max(ScreenSize, vec2(1.0));
    vec4 distortedColor = sceneAt(sourceUV);
    fragColor = vec4(distortedColor.rgb, originalColor.a);
}
