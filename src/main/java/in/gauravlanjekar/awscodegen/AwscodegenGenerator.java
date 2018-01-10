package in.gauravlanjekar.awscodegen;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import io.swagger.codegen.*;
import io.swagger.models.*;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import io.swagger.models.properties.StringProperty;
import io.swagger.util.Yaml;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Pattern;

public class AwscodegenGenerator extends DefaultCodegen implements CodegenConfig {

    // source folder where to write the files
    protected String sourceFolder = "src";
    protected String apiVersion = "1.0.0";
    protected String implFolder = "service";
    protected int serverPort = 8080;
    protected String projectName = "swagger-aws-server";


    public AwscodegenGenerator() {
        super();

        // set the output folder here
        outputFolder = "generated-code/awscodegen";

        /*
         * Models.  You can write model files using the modelTemplateFiles map.
         * if you want to create one template for file, you can do so here.
         * for multiple files for model, just put another entry in the `modelTemplateFiles` with
         * a different extension
         */

        modelTemplateFiles.put(
                "model.mustache", "_def.js"
        );


        /*
         * Api classes.  You can write classes for each Api file with the apiTemplateFiles map.
         * as with models, add multiple entries with different extensions for multiple files per
         * class
         */
        apiTemplateFiles.put(
                "controller.mustache",   // the template to use
                ".js");       // the extension for each file to write

        /*
         * Template Location.  This is the location which templates will be read from.  The generator
         * will use the resource stream to attempt to read the templates.
         */
        embeddedTemplateDir = templateDir = "awsCodegen";

        /*
         * Reserved words.  Override this with reserved words specific to your language
         */
        setReservedWordsLowerCase(
                Arrays.asList(
                        "break", "case", "class", "catch", "const", "continue", "debugger",
                        "default", "delete", "do", "else", "export", "extends", "finally",
                        "for", "function", "if", "import", "in", "instanceof", "let", "new",
                        "return", "super", "switch", "this", "throw", "try", "typeof", "var",
                        "void", "while", "with", "yield")
        );

        /*
         * Additional Properties.  These values can be passed to the templates and
         * are available in models, apis, and supporting files
         */
        additionalProperties.put("apiVersion", apiVersion);
        additionalProperties.put("serverPort", serverPort);
        additionalProperties.put("implFolder", implFolder);

        supportingFiles.add(new SupportingFile("writer.mustache", ("utils").replace(".", "/"), "writer.js"));


    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> getOperations(Map<String, Object> objs) {
        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, Object> apiInfo = (Map<String, Object>) objs.get("apiInfo");
        List<Map<String, Object>> apis = (List<Map<String, Object>>) apiInfo.get("apis");
        for (Map<String, Object> api : apis) {
            result.add((Map<String, Object>) api.get("operations"));
        }
        return result;
    }

    private static List<Map<String, Object>> sortOperationsByPath(List<CodegenOperation> ops) {
        Multimap<String, CodegenOperation> opsByPath = ArrayListMultimap.create();

        for (CodegenOperation op : ops) {
            opsByPath.put(op.path, op);
        }

        List<Map<String, Object>> opsByPathList = new ArrayList<Map<String, Object>>();
        for (Map.Entry<String, Collection<CodegenOperation>> entry : opsByPath.asMap().entrySet()) {
            Map<String, Object> opsByPathEntry = new HashMap<String, Object>();
            opsByPathList.add(opsByPathEntry);
            opsByPathEntry.put("path", entry.getKey());
            opsByPathEntry.put("operation", entry.getValue());
            List<CodegenOperation> operationsForThisPath = Lists.newArrayList(entry.getValue());
            operationsForThisPath.get(operationsForThisPath.size() - 1).hasMore = false;
            if (opsByPathList.size() < opsByPath.asMap().size()) {
                opsByPathEntry.put("hasMore", "true");
            }
        }

        return opsByPathList;
    }

    @Override
    public String apiPackage() {
        return "controllers";
    }


    @Override
    public String modelPackage() {
        return "models.interfaces";
    }


    /**
     * Configures the type of generator.
     *
     * @return the CodegenType for this generator
     * @see io.swagger.codegen.CodegenType
     */
    @Override
    public CodegenType getTag() {
        return CodegenType.SERVER;
    }

    /**
     * Configures a friendly name for the generator.  This will be used by the generator
     * to select the library with the -l flag.
     *
     * @return the friendly name for the generator
     */
    @Override
    public String getName() {
        return "aws-server";
    }

    /**
     * Returns human-friendly help for the generator.  Provide the consumer with help
     * tips, parameters here
     *
     * @return A string value for the help message
     */
    @Override
    public String getHelp() {
        return "Generates an api running in AWS  using the swagger-tools project.  By default, " +
                "it will also generate service classes--which you can disable with the `-Dnoservice` environment variable.";
    }

    @Override
    public String toApiName(String name) {
        if (name.length() == 0) {
            return "DefaultController";
        }
        return initialCaps(name);
    }

    @Override
    public String toApiFilename(String name) {
        return toApiName(name);
    }

    @Override
    public String apiFilename(String templateName, String tag) {
        String result = super.apiFilename(templateName, tag);

        if (templateName.equals("service.mustache")) {
            String stringToMatch = File.separator + "controllers" + File.separator;
            String replacement = File.separator + implFolder + File.separator;
            result = result.replaceAll(Pattern.quote(stringToMatch), replacement);
        }
        return result;
    }

    private String implFileFolder(String output) {
        return outputFolder + "/" + output + "/" + apiPackage().replace('.', '/');
    }

    /**
     * Escapes a reserved word as defined in the `reservedWords` array. Handle escaping
     * those terms here.  This logic is only called if a variable matches the reserved words
     *
     * @return the escaped term
     */
    @Override
    public String escapeReservedWord(String name) {
        if (this.reservedWordsMappings().containsKey(name)) {
            return this.reservedWordsMappings().get(name);
        }
        return "_" + name;
    }

    /**
     * Location to write api files.  You can use the apiPackage() as defined when the class is
     * instantiated
     */
    @Override
    public String apiFileFolder() {
        return outputFolder + File.separator + apiPackage().replace('.', File.separatorChar);
    }

    @Override
    public Map<String, Object> postProcessOperations(Map<String, Object> objs) {
        @SuppressWarnings("unchecked")
        Map<String, Object> objectMap = (Map<String, Object>) objs.get("operations");
        @SuppressWarnings("unchecked")
        List<CodegenOperation> operations = (List<CodegenOperation>) objectMap.get("operation");
        for (CodegenOperation operation : operations) {
            operation.httpMethod = operation.httpMethod.toLowerCase();

            List<CodegenParameter> params = operation.allParams;
            if (params != null && params.size() == 0) {
                operation.allParams = null;
            }
            List<CodegenResponse> responses = operation.responses;
            if (responses != null) {
                for (CodegenResponse resp : responses) {
                    if ("0".equals(resp.code)) {
                        resp.code = "default";
                    }
                }
            }
            if (operation.examples != null && !operation.examples.isEmpty()) {
                // Leave application/json* items only
                for (Iterator<Map<String, String>> it = operation.examples.iterator(); it.hasNext(); ) {
                    final Map<String, String> example = it.next();
                    final String contentType = example.get("contentType");
                    if (contentType == null || !contentType.startsWith("application/json")) {
                        it.remove();
                    }
                }
            }
        }
        return objs;
    }

    @Override
    public void processOpts() {
        super.processOpts();



        /*
         * Supporting Files.  You can write single files for the generator with the
         * entire object tree available.  If the input file has a suffix of `.mustache
         * it will be processed by the template engine.  Otherwise, it will be copied
         */
        // supportingFiles.add(new SupportingFile("controller.mustache",
        //   "controllers",
        //   "controller.js")
        // );
        supportingFiles.add(new SupportingFile("swagger.mustache",
                "api",
                "swagger.yaml")
        );

        supportingFiles.add(new SupportingFile("model-dynamoose-wrapper.mustache", "models", "dynamoose-wrapper.js"));

        writeOptional(outputFolder, new SupportingFile("index.mustache", "", "index.js"));
        writeOptional(outputFolder, new SupportingFile("package.mustache", "", "package.json"));
        writeOptional(outputFolder, new SupportingFile("README.mustache", "", "README.md"));
        if (System.getProperty("noservice") == null) {
            apiTemplateFiles.put(
                    "service.mustache",   // the template to use
                    "Service.js");       // the extension for each file to write
        }
    }

    @Override
    public void preprocessSwagger(Swagger swagger) {
        String host = swagger.getHost();
        String port = "8080";
        if (host != null) {
            String[] parts = host.split(":");
            if (parts.length > 1) {
                port = parts[1];
            }
        }
        this.additionalProperties.put("serverPort", port);

        if (swagger.getInfo() != null) {
            Info info = swagger.getInfo();
            if (info.getTitle() != null) {
                // when info.title is defined, use it for projectName
                // used in package.json
                projectName = info.getTitle()
                        .replaceAll("[^a-zA-Z0-9]", "-")
                        .replaceAll("^[-]*", "")
                        .replaceAll("[-]*$", "")
                        .replaceAll("[-]{2,}", "-")
                        .toLowerCase();
                this.additionalProperties.put("projectName", projectName);
            }
        }


        // need vendor extensions for x-swagger-router-controller
        Map<String, Path> paths = swagger.getPaths();
        if (paths != null) {
            for (String pathname : paths.keySet()) {
                Path path = paths.get(pathname);
                Map<HttpMethod, Operation> operationMap = path.getOperationMap();

                String tagForOption = "default";

                if (operationMap != null) {
                    for (HttpMethod method : operationMap.keySet()) {
                        Operation operation = operationMap.get(method);
                        String tag = "default";
                        if (operation.getTags() != null && operation.getTags().size() > 0) {
                            tag = toApiName(operation.getTags().get(0));
                            tagForOption = tag;
                        }
                        if (operation.getOperationId() == null) {
                            operation.setOperationId(getOrGenerateOperationId(operation, pathname, method.toString()));
                        }
                        if (operation.getVendorExtensions().get("x-swagger-router-controller") == null) {
                            operation.getVendorExtensions().put("x-swagger-router-controller", sanitizeTag(tag));
                        }
                        if (method.equals(HttpMethod.GET)) {
                            //add a model binding to the method.
                            if (operation.getResponses() != null && operation.getResponses().size() > 0) {
                                Response okResponse = operation.getResponses().get("200");
                                String type = okResponse.getSchema().getType();
                                Property property = okResponse.getSchema();
                                if ("array".equals(type)) {
                                    ArrayProperty arrayProperty = (ArrayProperty) property;
                                    type = arrayProperty.getItems().getType();
                                    property = arrayProperty.getItems();
                                }
                                if ("ref".equals(type)) {
                                    RefProperty refProperty = (RefProperty) property;
                                    type = refProperty.getSimpleRef();
                                }
                                operation.getVendorExtensions().put("x-aws-cruds-model-type", type);
                            }
                        }
                        switch (method){
                            case GET:
                                operation.getVendorExtensions().put("x-aws-cruds-is-get", true);
                                break;
                            case PUT:
                                operation.getVendorExtensions().put("x-aws-cruds-is-put", true);
                                break;
                            case DELETE:
                                operation.getVendorExtensions().put("x-aws-cruds-is-delete", true);
                                break;
                            case POST:
                                operation.getVendorExtensions().put("x-aws-cruds-is-post", true);
                                break;
                        }
                    }
                }

                //Add a options method.
                if (!optionsExists(operationMap)) {
                    //TODO: Check of cors option
                    Operation awsOptionsOperation = getOptionsOperation(tagForOption);
                    operationMap.put(HttpMethod.OPTIONS, awsOptionsOperation);
                    path.options(awsOptionsOperation);
                }
            }
        }
    }

    private Operation getOptionsOperation(String tag) {
        Operation operation = new Operation();
        operation.setConsumes(Arrays.asList("application/json"));
        operation.setProduces(Arrays.asList("application/json"));
        operation.setTags(Arrays.asList(tag));
        operation.setVendorExtension("x-ignored", "true");

        Response response = new Response();
        response.setDescription("200 response for cors support");
        RefProperty responseObjectProperty = new RefProperty();
        responseObjectProperty.set$ref("#/definitions/Empty");
        response.setSchema(responseObjectProperty);
        response.setHeaders(
                ImmutableMap.<String, Property>of("Access-Control-Allow-Origin", new StringProperty(),
                        "Access-Control-Allow-Methods", new StringProperty(),
                        "Access-Control-Allow-Headers", new StringProperty()));

        XAmazonApigatewayIntegration xAmazonApigatewayIntegration = new XAmazonApigatewayIntegration();
        Responses responses = new Responses();
        Default defaultResponse = new Default();
        defaultResponse.setStatusCode("200");
        ResponseParameters responseParameters = new ResponseParameters();
        responseParameters
                .setMethodResponseHeaderAccessControlAllowHeaders("Content-Type,Authorization,X-Amz-Date,X-Api-Key,X-Amz-Security-Token");
        responseParameters
                .setMethodResponseHeaderAccessControlAllowMethods("DELETE,GET,HEAD,OPTIONS,PATCH,POST,PUT");
        responseParameters.setMethodResponseHeaderAccessControlAllowOrigin("*");

        RequestTemplates requestTemplates = new RequestTemplates();
        requestTemplates.setApplicationJson("{\"statusCode\": 200}");

        xAmazonApigatewayIntegration.setPassthroughBehavior("passthroughBehavior");
        xAmazonApigatewayIntegration.setType("mock");

        xAmazonApigatewayIntegration.setRequestTemplates(requestTemplates);
        defaultResponse.setResponseParameters(responseParameters);
        responses.setDefault(defaultResponse);

        xAmazonApigatewayIntegration.setResponses(responses);

        response.setVendorExtension("x-amazon-apigateway-integration", xAmazonApigatewayIntegration);

        operation.setResponses(ImmutableMap.<String, Response>of("200", response));

        return operation;
    }

    private boolean optionsExists(Map<HttpMethod, Operation> operationMap) {
        return operationMap.containsKey(HttpMethod.OPTIONS);
    }

    @Override
    public Map<String, Object> postProcessSupportingFileData(Map<String, Object> objs) {
        Swagger swagger = (Swagger) objs.get("swagger");
        if (swagger != null) {
            try {
                SimpleModule module = new SimpleModule();
                module.addSerializer(Double.class, new JsonSerializer<Double>() {
                    @Override
                    public void serialize(Double val, JsonGenerator jgen,
                                          SerializerProvider provider) throws IOException, JsonProcessingException {
                        jgen.writeNumber(new BigDecimal(val));
                    }
                });
                objs.put("swagger-yaml", Yaml.mapper().registerModule(module).writeValueAsString(swagger));
            } catch (JsonProcessingException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
        for (Map<String, Object> operations : getOperations(objs)) {
            @SuppressWarnings("unchecked")
            List<CodegenOperation> ops = (List<CodegenOperation>) operations.get("operation");

            List<Map<String, Object>> opsByPathList = sortOperationsByPath(ops);
            operations.put("operationsByPath", opsByPathList);
        }
        return super.postProcessSupportingFileData(objs);
    }

    @Override
    public String removeNonNameElementToCamelCase(String name) {
        return removeNonNameElementToCamelCase(name, "[-:;#]");
    }

    @Override
    public String escapeUnsafeCharacters(String input) {
        return input.replace("*/", "*_/").replace("/*", "/_*");
    }

    @Override
    public String escapeQuotationMark(String input) {
        // remove " to avoid code injection
        return input.replace("\"", "");
    }
}