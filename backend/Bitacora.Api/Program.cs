using Bitacora.Api;
using Bitacora.Application;
using Bitacora.Infrastructure;
using Microsoft.EntityFrameworkCore;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.IdentityModel.Tokens;
using System.Threading.RateLimiting;

var builder = WebApplication.CreateBuilder(args);
builder.Services.AddControllers(o => o.ReturnHttpNotAcceptable = true);
LabAuthentication.PrepareDevelopmentAccount(builder);
var signingKey = LabAuthentication.SigningKey(builder.Configuration);
builder.Services.AddSingleton<LabAuthentication>();
builder.Services.AddAuthentication(JwtBearerDefaults.AuthenticationScheme).AddJwtBearer(options => {
    options.MapInboundClaims = false;
    options.IncludeErrorDetails = false;
    options.TokenValidationParameters = new TokenValidationParameters {
        ValidateIssuerSigningKey = true, IssuerSigningKey = signingKey,
        ValidateIssuer = true, ValidIssuer = LabAuthentication.Issuer,
        ValidateAudience = true, ValidAudience = LabAuthentication.Audience,
        ValidateLifetime = true, RequireExpirationTime = true, RequireSignedTokens = true,
        ValidAlgorithms = [SecurityAlgorithms.HmacSha256], ClockSkew = TimeSpan.Zero
    };
});
builder.Services.AddAuthorization();
builder.Services.AddRateLimiter(options => {
    options.RejectionStatusCode = 429;
    options.AddPolicy("login", context => RateLimitPartition.GetFixedWindowLimiter(
        context.Connection.RemoteIpAddress?.ToString() ?? "local", _ => new FixedWindowRateLimiterOptions {
            PermitLimit = 20, Window = TimeSpan.FromMinutes(1), QueueLimit = 0
        }));
});
builder.Services.AddOpenApi(options => options.AddDocumentTransformer(JwtOpenApi.Transform));
builder.Services.AddProblemDetails();
builder.Services.AddExceptionHandler<GlobalExceptionHandler>();
var dbPath = Path.Combine(builder.Environment.ContentRootPath, "bitacora.db");
builder.Services.AddDbContext<BitacoraDbContext>(o => o.UseSqlite(
    builder.Configuration.GetConnectionString("Bitacora") ?? $"Data Source={dbPath}"));
builder.Services.AddScoped<IRegistroRepository, RegistroRepository>();
builder.Services.AddScoped<RegistroService>();
var app = builder.Build();
app.UseExceptionHandler();
app.UseStatusCodePages();
app.UseAuthentication();
app.UseAuthorization();
app.UseRateLimiter();
if (app.Environment.IsDevelopment())
{
    app.MapOpenApi();
    app.UseSwaggerUI(o => o.SwaggerEndpoint("/openapi/v1.json", "Bitácora v1"));
    // Development-only fault injection for task 2.5. No production registration.
    app.MapGet("/api/pruebas/demora", async (CancellationToken ct) => {
        await Task.Delay(TimeSpan.FromSeconds(15), ct);
        return Results.Ok(Array.Empty<RegistroListItem>());
    }).RequireAuthorization();
    app.MapGet("/api/pruebas/error", (Func<IResult>)(() => throw new InvalidOperationException("Fallo de prueba controlado"))).RequireAuthorization();
}
app.MapControllers();
app.Run();
