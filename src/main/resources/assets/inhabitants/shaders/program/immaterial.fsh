#version 150

uniform sampler2D DiffuseSampler;
in vec2 texCoord;

uniform float Time;
uniform vec2 InSize;

out vec4 fragColor;

// 2 * PI, this must be a constant
const float TWO_PI = 6.28318530718;

vec2 distortedCoord;
vec4 finalColor;

void wave() {
    float waveSpeed = 1.0;
    float waveStrength = 0.0015; 
    float waveDensity = 3.0;
    
    float t = Time * TWO_PI * waveSpeed;
    
    float drift = sin(texCoord.y * (2.0 * waveDensity) + t) * waveStrength;
    float interference = sin((texCoord.x + texCoord.y) * (4.0 * waveDensity) + t) * (waveStrength * 0.6);
    float shimmer = cos(texCoord.x * (6.0 * waveDensity) - t) * (waveStrength * 0.4);
    
    vec2 offset = vec2(drift + interference, interference + shimmer);
    distortedCoord = texCoord + offset;
}

void refractit() {
    float shimmerIntensity = 0.0012;
    float shimmerScale = 60.0;
    float t = Time * TWO_PI;
    
    distortedCoord.x += sin(distortedCoord.y * shimmerScale + t) * shimmerIntensity;
    distortedCoord.y += cos(distortedCoord.x * shimmerScale - t) * shimmerIntensity;
}

void blur() {
    float blurRadius = 4.0; 
    vec2 texelSize = 1.0 / InSize;

    finalColor = texture(DiffuseSampler, distortedCoord) * 0.4;
    finalColor += texture(DiffuseSampler, distortedCoord + vec2(texelSize.x * blurRadius, 0.0)) * 0.15;
    finalColor += texture(DiffuseSampler, distortedCoord - vec2(texelSize.x * blurRadius, 0.0)) * 0.15;
    finalColor += texture(DiffuseSampler, distortedCoord + vec2(0.0, texelSize.y * blurRadius)) * 0.15;
    finalColor += texture(DiffuseSampler, distortedCoord - vec2(0.0, texelSize.y * blurRadius)) * 0.15;
}

void purpurpurpurpurpurpur() {
    float lum = dot(finalColor.rgb, vec3(0.299, 0.587, 0.114));
    
    // heart beat
    float heartRate = 1.0; 
    float pulse = sin(Time * TWO_PI * heartRate) * 0.5 + 0.5;
    pulse = pow(pulse, 4.0); 
    
    // dead world (oooooooh)
    finalColor.rgb = mix(finalColor.rgb, vec3(lum), 0.70);
    
    vec3 deep = vec3(0.08, 0.02, 0.15);    
    vec3 harsh = vec3(0.45, 0.15, 0.40);  
    
    vec3 purpleGrade = mix(deep, harsh, lum);
    
    // pulsing vignette
    vec2 tc = texCoord - vec2(0.5);
    float vignetteDist = length(tc);
    float vignette = vignetteDist * (1.2 + pulse * 0.5);
    vignette = pow(vignette, 3.0);
    
    float tintIntensity = mix(0.45, 0.2, lum); 
    finalColor.rgb = mix(finalColor.rgb, purpleGrade, tintIntensity * (0.8 + vignette));
    
    finalColor.rgb = smoothstep(-0.05, 1.05, finalColor.rgb);
}

void main() {
    wave();
    refractit();
    blur();
    purpurpurpurpurpurpur();
    fragColor = vec4(finalColor.rgb, 1.0);
}