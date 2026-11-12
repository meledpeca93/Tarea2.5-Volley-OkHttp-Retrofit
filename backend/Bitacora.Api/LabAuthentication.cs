using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using Microsoft.AspNetCore.Identity;
using Microsoft.IdentityModel.Tokens;

namespace Bitacora.Api;

// Local classroom issuer, registered only in Development. Not a production IdP.
public sealed class LabAuthentication(IConfiguration configuration)
{
    public const string Issuer = "Bitacora.Laboratorio";
    public const string Audience = "Bitacora.Android";
    public static void PrepareDevelopmentAccount(WebApplicationBuilder builder)
    {
        if (!builder.Environment.IsDevelopment()) return;
        var path = Path.Combine(builder.Environment.ContentRootPath, ".dev-auth.json");
        if (!File.Exists(path))
        {
            var password = Convert.ToHexString(RandomNumberGenerator.GetBytes(12));
            var hash = new PasswordHasher<string>().HashPassword("estudiante", password);
            File.WriteAllText(path, JsonSerializer.Serialize(new {
                Jwt = new { SigningKey = Convert.ToBase64String(RandomNumberGenerator.GetBytes(48)) },
                LabAccount = new { Username = "estudiante", PasswordHash = hash }
            }, new JsonSerializerOptions { WriteIndented = true }));
            if (!OperatingSystem.IsWindows()) File.SetUnixFileMode(path, UnixFileMode.UserRead | UnixFileMode.UserWrite);
            var credentials = Path.Combine(builder.Environment.ContentRootPath, ".dev-login.json");
            File.WriteAllText(credentials, JsonSerializer.Serialize(new { username = "estudiante", password },
                new JsonSerializerOptions { WriteIndented = true }));
            if (!OperatingSystem.IsWindows()) File.SetUnixFileMode(credentials, UnixFileMode.UserRead | UnixFileMode.UserWrite);
        }
        // Environment variables take precedence over the local development file.
        // Read explicitly: the default PhysicalFileProvider excludes dot-prefixed files.
        using var source = File.OpenRead(path);
        builder.Configuration.AddJsonStream(source).AddEnvironmentVariables();
    }
    public static SymmetricSecurityKey SigningKey(IConfiguration config)
    {
        var value = config["Jwt:SigningKey"];
        if (string.IsNullOrEmpty(value) || Encoding.UTF8.GetByteCount(value) < 32)
            throw new InvalidOperationException("Configure Jwt:SigningKey con al menos 32 bytes.");
        return new SymmetricSecurityKey(Encoding.UTF8.GetBytes(value));
    }
    public LoginReply? Login(LoginInput input)
    {
        var username = configuration["LabAccount:Username"];
        var hash = configuration["LabAccount:PasswordHash"];
        if (username is null || hash is null) return null;
        // Verify the hash even for an unknown username.
        var result = new PasswordHasher<string>().VerifyHashedPassword(username, hash, input.Password);
        if (result == PasswordVerificationResult.Failed || input.Username != username) return null;
        var now = DateTime.UtcNow;
        var expires = now.AddMinutes(15);
        var jwt = new JwtSecurityToken(Issuer, Audience, [
            new Claim(JwtRegisteredClaimNames.Sub, username),
            new Claim(JwtRegisteredClaimNames.Jti, Guid.NewGuid().ToString()),
            new Claim(JwtRegisteredClaimNames.Iat, new DateTimeOffset(now).ToUnixTimeSeconds().ToString(), ClaimValueTypes.Integer64),
            new Claim("client_id", "bitacora-android")
        ], now, expires, new SigningCredentials(SigningKey(configuration), SecurityAlgorithms.HmacSha256));
        return new LoginReply(new JwtSecurityTokenHandler().WriteToken(jwt), expires, username);
    }
}
public sealed class LoginInput
{
    [System.ComponentModel.DataAnnotations.Required, System.ComponentModel.DataAnnotations.StringLength(100)]
    public string Username { get; init; } = "";
    [System.ComponentModel.DataAnnotations.Required, System.ComponentModel.DataAnnotations.StringLength(128)]
    public string Password { get; init; } = "";
}
public sealed record LoginReply(string AccessToken, DateTime ExpiresAtUtc, string Username);
