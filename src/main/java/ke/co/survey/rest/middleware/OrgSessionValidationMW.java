package ke.co.survey.rest.middleware;

import io.undertow.server.HttpServerExchange;
import ke.co.skyworld.ancillaries.authentication_manager.beans.AccessTokenValidity;
import ke.co.skyworld.ancillaries.query_manager.beans.FlexicoreArrayList;
import ke.co.skyworld.ancillaries.query_manager.beans.FlexicoreHashMap;
import ke.co.skyworld.ancillaries.query_manager.beans.TransactionWrapper;
import ke.co.skyworld.ancillaries.query_manager.query.FilterPredicate;
import ke.co.skyworld.ancillaries.query_manager.query.QueryBuilder;
import ke.co.skyworld.ancillaries.query_repository.Repository;
import ke.co.skyworld.ancillaries.restful_undertow.beans.UserSessionContext;
import ke.co.skyworld.ancillaries.restful_undertow.handlers.ExchangeMiddleware;
import ke.co.skyworld.ancillaries.restful_undertow.interfaces.ExchangeHandler;
import ke.co.skyworld.ancillaries.restful_undertow.utils.ExchangeResponse;
import ke.co.skyworld.ancillaries.restful_undertow.utils.ExchangeUtils;
import ke.co.skyworld.ancillaries.utility_items.DateTime;
import ke.co.skyworld.ancillaries.utility_items.Misc;
import ke.co.skyworld.ancillaries.utility_items.constants.StringRefs;
import ke.co.skyworld.ancillaries.utility_items.data_formatting.XmlUtils;
import ke.co.skyworld.ancillaries.utility_items.security.ScedarUID;
import org.w3c.dom.Document;

import static ke.co.skyworld.ancillaries.utility_items.DateTime.getDelay;

public class OrgSessionValidationMW extends ExchangeMiddleware {
    private final ExchangeHandler nextHandler;

    public OrgSessionValidationMW(ExchangeHandler nextHandler) {
        this.nextHandler = nextHandler;
    }
    @Override
    public void handleRequest(HttpServerExchange exchange, boolean[] breakChain, Object... otherParams) throws Exception {
        String authorizationToken, requestReference;
        String organizationId = StringRefs.SENTINEL;

        UserSessionContext userSessionContext = new UserSessionContext();
        userSessionContext.setExchange(exchange);

        try {
            authorizationToken = exchange.getRequestHeaders().get("access_token").getFirst().replace("Bearer ", "");
        }catch (Exception e){
            ExchangeResponse.sendInternalServerError(exchange,Misc.getTransactionWrapperStackTrace(e));
            return;
        }


        //TODO:request reference
        requestReference = ScedarUID.generateUid(20);
        String clientIPAddress = exchange.getRequestHeaders().get("X-Real-IP").getFirst();

       /* try {
            authorizationToken = HashUtils.base64Decode(authorizationToken);
        } catch (Exception e) {
            ExchangeResponse.sendBadRequest(exchange, "Wrong format used for authorization.", "Unrecognized format for authorization.");
            breakChain[0] = true;
            return;
        }*/
/*
        String[] authArray = authorizationToken.split("::");
        if (authArray.length < 2) {
            ExchangeResponse.sendBadRequest(exchange, "Missing one of two entities: Organization Id or Access Token.", "Organization Id and Access Token must be provided in authorization");
            breakChain[0] = true;
            return;
        }
        String organizationId = authArray[0];
        String accessToken = authArray[1];*/

        TransactionWrapper<AccessTokenValidity> accessTokenValidityWrapper = validateToken(organizationId, authorizationToken, clientIPAddress);

        if (accessTokenValidityWrapper.getData().equals(AccessTokenValidity.INVALID)) {
            ExchangeResponse.sendUnauthorized(exchange, accessTokenValidityWrapper.getErrors(), accessTokenValidityWrapper.getMessages());
            breakChain[0] = true;
            return;
        }

        if(accessTokenValidityWrapper.hasErrors()){
            accessTokenValidityWrapper.addMessage(accessTokenValidityWrapper.getErrors());
             ExchangeResponse.sendUnauthorized(exchange,accessTokenValidityWrapper.getErrors());
            return;
        }

        System.out.println("authorizationToken:"+authorizationToken);
        try {
            userSessionContext.put(StringRefs.REQ_ORGANIZATION_ID, organizationId);

            TransactionWrapper<FlexicoreHashMap> userAccountDetailsWrapper = fetchTokenUserAccountDetails(organizationId, authorizationToken);

            if (userAccountDetailsWrapper.hasErrors()) {
                ExchangeResponse.sendInternalServerError(exchange, userAccountDetailsWrapper, "An error occurred while authenticating your request.");
                breakChain[0] = true;
                return;
            }

            FlexicoreHashMap accountDetailsWrapperData = userAccountDetailsWrapper.getData();

            FlexicoreHashMap userAccountMap = accountDetailsWrapperData.getValue("user_account_details");
            FlexicoreHashMap authenticationProfileDetails = accountDetailsWrapperData.getValue("authentication_profile_details");

            userSessionContext.setUserAccountDetails(userAccountMap);
            userSessionContext.setUserProfileDetails(authenticationProfileDetails);

            userSessionContext.put(StringRefs.REQUEST_REFERENCE, requestReference);
            userSessionContext.put(StringRefs.REQ_ACCESS_TOKEN, authorizationToken);
            userSessionContext.put(StringRefs.REQ_CLIENT_IP_ADDRESS, clientIPAddress);
            userSessionContext.put(StringRefs.REQ_USER_ID, userAccountMap.getStringValue("user_id"));
            userSessionContext.put(StringRefs.REQ_USER_NAME, userAccountMap.getValue("user_name"));

        }
        catch (Exception e) {
            ExchangeResponse.sendInternalServerError(exchange, Misc.getTransactionWrapperStackTrace(e), "An error occurred while authenticating your request.");
            breakChain[0] = true;
            return;
        }

        String requestBody = ExchangeUtils.getRequestBody(exchange);
        userSessionContext.put(StringRefs.REQ_BODY, requestBody);

        if (nextHandler instanceof ExchangeMiddleware middleware) {
            middleware.handleRequest(exchange, breakChain, userSessionContext);
        } else {
            nextHandler.handleRequest(exchange, userSessionContext);
        }
    }

    @Override
    public ExchangeHandler getNextHandler() {
        return nextHandler;
    }

    public static TransactionWrapper<AccessTokenValidity> validateToken(String organizationId, String accessToken, String clientIPAddress) {

        try {
            TransactionWrapper<AccessTokenValidity> transactionWrapper = new TransactionWrapper<>();
            TransactionWrapper<FlexicoreHashMap> tokenDetailsWrapper = fetchTokenDetails(organizationId, accessToken);
            if (tokenDetailsWrapper.hasErrors()) {
                transactionWrapper.setHasErrors(true);
                transactionWrapper.setErrors(tokenDetailsWrapper.getErrorsAsList());
                transactionWrapper.setData(AccessTokenValidity.INVALID);
                return transactionWrapper;
            }

            FlexicoreHashMap tokenDetails = tokenDetailsWrapper.getSingleRecord();
            if (tokenDetails == null || tokenDetails.isEmpty()) {
                transactionWrapper.setData(AccessTokenValidity.INVALID);
                transactionWrapper.setErrorTitle("Invalid credentials");
                transactionWrapper.addError("Expired/Corrupt access token");
                return transactionWrapper;
            }


            String[] authIpAddresses = tokenDetails.getValue("auth_ip_address").toString().split(",");
            boolean isAcceptedIp = false;
            for (String authIpAddress : authIpAddresses) {
                if (authIpAddress.trim().equals(clientIPAddress)) {
                    isAcceptedIp = true;
                    break;
                }
            }

            if (!isAcceptedIp) {
                transactionWrapper.setData(AccessTokenValidity.INVALID);
                transactionWrapper.addError("Remote address used to log in differs from current remote address. Kindly login again");
                transactionWrapper.setErrorTitle("Access Denied.");
                return transactionWrapper;
            }

            long accessTokenTime = tokenDetails.getDateValue("date_created").getTime();

            long currentTime = DateTime.getCurrentUnixTimestamp();
            long delay = getDelay(tokenDetails.getValue("time_unit"),
                    Long.parseLong(tokenDetails.getValue("time_to_live").toString()));

            if (accessTokenTime + delay <= currentTime) {
                System.out.println("accessToken:"+accessToken);
                revokeToken(organizationId, accessToken);
                transactionWrapper.setData(AccessTokenValidity.INVALID);
                transactionWrapper.addError("Access Token provided is expired.");
                transactionWrapper.setErrorTitle("Access Denied.");
                transactionWrapper.addMessage("Access Token provided is expired.");
            } else {
                transactionWrapper.setData(AccessTokenValidity.VALID);
                transactionWrapper.addMessage("Login Successful.");
            }

            return transactionWrapper;

        } catch (Exception e) {
            System.err.println("Possible Line Number: " + e.getStackTrace()[0].getLineNumber());
            return Misc.getTransactionWrapperStackTrace(e);
        }
    }



    public static TransactionWrapper<FlexicoreHashMap> fetchTokenDetails(String organizationId, String accessToken) {
        try {
            QueryBuilder queryBuilder = new QueryBuilder()
                    .select()
                    .selectColumn("at.*, ua.surname, ua.first_name, ua.username, ua.primary_email_address, ua.authentication_profile_code")
                    .from()
                    .joinPhrase("vendors.access_tokens at LEFT JOIN vendors.user_accounts ua on ua.user_id = at.user_id")
                    .where("at.access_token = :access_token");

            return Repository.joinSelectQuery(organizationId, queryBuilder, new FlexicoreHashMap().addQueryArgument(":access_token", accessToken));
        } catch (Exception e) {
            System.err.println("Possible Line Number: " + e.getStackTrace()[0].getLineNumber());
            return Misc.getTransactionWrapperStackTrace(e);
        }
    }

    public static TransactionWrapper<FlexicoreHashMap> fetchTokenUserAccountDetails(String organizationId, String accessToken) {
        try {
            TransactionWrapper<FlexicoreHashMap> transactionWrapper = new TransactionWrapper<>();
            FlexicoreHashMap allDetails = new FlexicoreHashMap();

            {
                TransactionWrapper<FlexicoreHashMap> tempWrapper = Repository.selectWhere(organizationId,
                        "vendors.access_tokens",
                        new FilterPredicate("access_token = :access_token"),
                        new FlexicoreHashMap().addQueryArgument(":access_token", accessToken));

                if (tempWrapper.hasErrors()) {
                    return tempWrapper;
                }


                allDetails.putValue("access_token_details", tempWrapper.getSingleRecord());
            }

            FlexicoreHashMap userAccountDetails;
            {
                QueryBuilder queryBuilder = new QueryBuilder()
                        .select()
                        .selectColumn("""
                                ua.*""")
                        .from()
                        .joinPhrase("vendors.user_accounts as ua LEFT JOIN  vendors.access_tokens at on ua.user_id = at.user_id")
                        .where("at.access_token = :access_token");

                TransactionWrapper<FlexicoreHashMap> tempWrapper = Repository.joinSelectQuery(organizationId, queryBuilder, new FlexicoreHashMap().addQueryArgument(":access_token", accessToken));

                if (tempWrapper.hasErrors()) {
                    return tempWrapper;
                }

                userAccountDetails = tempWrapper.getSingleRecord();

                Document docAuthParams = XmlUtils.parseXml(userAccountDetails.getStringValue("authentication_parameters"));

                String passwordStatus = XmlUtils.getTagValue(docAuthParams, "/AUTHENTICATION_PARAMETERS/PORTAL/PASSWORD/@STATUS");
                String passwordStatusDate = XmlUtils.getTagValue(docAuthParams, "/AUTHENTICATION_PARAMETERS/PORTAL/PASSWORD/@STATUS_DATE");

                userAccountDetails.putValue("password_status", passwordStatus);
                userAccountDetails.putValue("password_status_date", passwordStatusDate);

                //maskUserAccountData(tempWrapper);

                userAccountDetails.removeColumn("previous_passwords");

                allDetails.putValue("user_account_details", userAccountDetails);
            }

            if (userAccountDetails != null && !userAccountDetails.isEmpty()) {
                QueryBuilder queryBuilder = new QueryBuilder()
                        .select()
                        .selectColumn("""
                                ap.*""")
                        .from()
                        .joinPhrase( "vendors.user_accounts ua\n" +
                                "         LEFT JOIN vendors.authentication_profiles ap on ua.authentication_profile_code = ap.profile_code")
                        .where("ua.user_id = :user_id");

                TransactionWrapper<FlexicoreHashMap> tempWrapper = Repository.joinSelectQuery(organizationId,
                        queryBuilder,
                        new FlexicoreHashMap()
                                .addQueryArgument(":user_id", userAccountDetails.get("user_id")));

                if (tempWrapper.hasErrors()) {
                    return tempWrapper;
                }

                allDetails.putValue("authentication_profile_details", tempWrapper.getSingleRecord());

            } else {
                allDetails.putValue("authentication_profile_details", new FlexicoreHashMap());
            }

            transactionWrapper.setData(allDetails);
            return transactionWrapper;
        } catch (Exception e) {
            System.err.println("Possible Line Number: " + e.getStackTrace()[0].getLineNumber());
            return Misc.getTransactionWrapperStackTrace(e);
        }
    }

    public static TransactionWrapper<FlexicoreHashMap> revokeToken(String organizationId, String accessToken) {
        System.out.println("inside revoke token:"+accessToken);
        TransactionWrapper<FlexicoreArrayList> transactionWrapper = Repository.delete(organizationId, "vendors.access_tokens",
                new FilterPredicate().equalTo("access_token", ":access_token"),
                new FlexicoreHashMap().addQueryArgument(":access_token", accessToken));

        TransactionWrapper<FlexicoreHashMap> wrapper = new TransactionWrapper<>();
        if (transactionWrapper.hasErrors()) {
            wrapper.copyFrom(transactionWrapper);
            return wrapper;
        }

        wrapper.setData(transactionWrapper.getSingleRecord());

        return wrapper;
    }
}
