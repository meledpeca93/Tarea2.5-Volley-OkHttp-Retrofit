using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.RateLimiting;
namespace Bitacora.Api.Controllers;

[ApiController]
[Route("api/auth")]
public sealed class AuthController(LabAuthentication auth, IWebHostEnvironment environment) : ControllerBase
{
    [HttpPost("login")]
    [AllowAnonymous]
    [EnableRateLimiting("login")]
    [ProducesResponseType<LoginReply>(200)]
    [ProducesResponseType<ValidationProblemDetails>(400)]
    [ProducesResponseType<ProblemDetails>(401)]
    [ProducesResponseType(429)]
    public IActionResult Login(LoginInput input)
    {
        if (!environment.IsDevelopment()) return NotFound();
        Response.Headers.CacheControl = "no-store";
        return auth.Login(input) is { } reply ? Ok(reply) : Problem(statusCode: 401, title: "Usuario o contraseña incorrectos.");
    }
}
