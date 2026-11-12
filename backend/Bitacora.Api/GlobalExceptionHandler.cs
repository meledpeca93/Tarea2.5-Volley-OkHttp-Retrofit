using Bitacora.Domain;
using Microsoft.AspNetCore.Diagnostics;
using Microsoft.AspNetCore.Mvc;
namespace Bitacora.Api;
public sealed class GlobalExceptionHandler(ILogger<GlobalExceptionHandler> logger) : IExceptionHandler
{
    public async ValueTask<bool> TryHandleAsync(HttpContext context, Exception error, CancellationToken ct)
    {
        ProblemDetails problem;
        if (error is RegistroValidationException invalid)
            problem = new ValidationProblemDetails(invalid.Errors) { Status = 400, Title = "Registro inválido." };
        else
        {
            logger.LogError(error, "Error no controlado. TraceId: {TraceId}", context.TraceIdentifier);
            problem = new ProblemDetails { Status = 500, Title = "Error interno del servidor.",
                Detail = "No se pudo completar la solicitud." };
        }
        problem.Extensions["traceId"] = context.TraceIdentifier;
        context.Response.StatusCode = problem.Status!.Value;
        await context.Response.WriteAsJsonAsync(problem, options: null, contentType: "application/problem+json", cancellationToken: ct);
        return true;
    }
}
