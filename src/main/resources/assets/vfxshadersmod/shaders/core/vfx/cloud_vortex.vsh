#version 150

in vec3 Position;
in vec2 UV0;

out vec2 texCoord;

void main() {
    // Fullscreen triangle-strip quad. The fragment shader traces the cloud volume.
    gl_Position = vec4(Position.xy, 0.0, 1.0);
    texCoord = UV0;
}
