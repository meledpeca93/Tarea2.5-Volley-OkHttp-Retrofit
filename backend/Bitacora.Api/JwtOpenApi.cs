using Microsoft.OpenApi;
namespace Bitacora.Api;
public static class JwtOpenApi
{
    public static Task Transform(OpenApiDocument document, Microsoft.AspNetCore.OpenApi.OpenApiDocumentTransformerContext context, CancellationToken ct)
    {
        document.Components ??= new OpenApiComponents();
        document.Components.SecuritySchemes ??= new Dictionary<string, IOpenApiSecurityScheme>();
        document.Components.SecuritySchemes["Bearer"] = new OpenApiSecurityScheme {
            Type = SecuritySchemeType.Http, Scheme = "bearer", BearerFormat = "JWT",
            Description = "JWT del login local de desarrollo. Pegar solo el token."
        };
        foreach (var (path, item) in document.Paths)
        {
            if (item.Operations is not null && (path.StartsWith("/api/registros") || path.StartsWith("/api/pruebas")))
                foreach (var operation in item.Operations.Values)
                {
                    operation.Security = [new OpenApiSecurityRequirement {
                        [new OpenApiSecuritySchemeReference("Bearer", document)] = []
                    }];
                    operation.Responses ??= new OpenApiResponses();
                    operation.Responses["401"] = new OpenApiResponse { Description = "JWT ausente, inválido o vencido." };
                }
        }
        return Task.CompletedTask;
    }
}
