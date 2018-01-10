
package in.gauravlanjekar.awscodegen;


public class XAmazonApigatewayIntegration {

    private Responses responses;
    private RequestTemplates requestTemplates;
    private String passthroughBehavior;
    private String type;

    public Responses getResponses() {
        return responses;
    }

    public void setResponses(Responses responses) {
        this.responses = responses;
    }

    public RequestTemplates getRequestTemplates() {
        return requestTemplates;
    }

    public void setRequestTemplates(RequestTemplates requestTemplates) {
        this.requestTemplates = requestTemplates;
    }

    public String getPassthroughBehavior() {
        return passthroughBehavior;
    }

    public void setPassthroughBehavior(String passthroughBehavior) {
        this.passthroughBehavior = passthroughBehavior;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

}
