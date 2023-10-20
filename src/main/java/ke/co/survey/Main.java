package ke.co.survey;


import ke.co.survey.UTILS.EncryptionDecryptionImp;
import ke.co.survey.rest.RestAPIServer;
import ke.co.skyworld.ancillaries.query_manager.beans.FlexicoreArrayList;
import ke.co.skyworld.ancillaries.query_manager.beans.FlexicoreHashMap;
import ke.co.skyworld.ancillaries.query_manager.beans.PageableWrapper;
import ke.co.skyworld.ancillaries.query_manager.beans.TransactionWrapper;
import ke.co.skyworld.ancillaries.query_manager.query.FilterPredicate;
import ke.co.skyworld.ancillaries.query_manager.query.QueryBuilder;
import ke.co.skyworld.ancillaries.query_repository.Repository;
import ke.co.skyworld.ancillaries.utility_items.DateTime;
import ke.co.skyworld.ancillaries.utility_items.constants.StringRefs;
import ke.co.skyworld.ancillaries.utility_items.enums.ReturnValue;
import ke.co.skyworld.ancillaries.utility_items.security.Encryption;

import java.util.Map;

public class Main {
    public static void main(String[] args) throws InterruptedException {

        //Provide your own encryption/decryption object as long as it implements the EncryptionInterface interface
        Encryption.setEncryptionImpl(new EncryptionDecryptionImp());

        //If line is included, it will validate the config file against its XSD to make sure it is ok. Though optional.
      /*  if (ConfigFile.validateConfigFile() == ReturnValue.ERROR) {
            System.err.println("Invalid Configuration File");
            System.exit(-1);
        }*/

        //Mandatory. Must be called to set up the Connection Manager before program can start.
        if (Repository.setup() == ReturnValue.ERROR) {
            System.err.println("Failed to set up Query Manager");
            System.exit(-1);
        }

        //For starting the Undertow Rest API.
        RestAPIServer.start();


//        sampleSelects();
        //sampleInserts();
        //sampleUpdate();
        //sampleDeletes();
    }

    private static void sampleSelects() {

        /**
         * Getting Unpaginated results.
         * --------------------------------------------
         * Will return all the records
         * */
        TransactionWrapper<FlexicoreArrayList> wrapper = Repository.select(StringRefs.SENTINEL, "master.master_users");
        if (wrapper.hasErrors()) {

            //Usually you'll return the errors as a response to an API call
            System.err.println(wrapper.getErrors());
            return;
        }

        FlexicoreArrayList resultArrayList = wrapper.getData();
        //In case you want to display the results of the output.
        resultArrayList.printRecordsVerticallyLabelled();

        //But you can also loop through the list and get the records.
        for (FlexicoreHashMap flexicoreHashMap : resultArrayList) {
            flexicoreHashMap.printRecordVerticalLabelled();

            for (Map.Entry<String, Object> entry : flexicoreHashMap.entrySet()) {
                String column = entry.getKey();
                Object value = entry.getValue();

                System.out.println("Column: "+ column+"  Value: "+ value);
            }
        }


        /**
         * Getting Paginated results.
         * --------------------------------------------
         * Will return the records with LIMIT and OFFSET
         * */

        TransactionWrapper<PageableWrapper<FlexicoreArrayList>> wrapper2 = Repository.select(StringRefs.SENTINEL, "master.users",
                new int[]{1,10} //[0]-Page, [1]-Page Size
        );
        if (wrapper.hasErrors()) {

            //Usually you'll return the errors as a response to an API call
            System.err.println(wrapper.getErrors());
            return;
        }

        //PageableWrapper contains the data and also includes more details like the page and page size. You can send it as a response to an API call.
        PageableWrapper<FlexicoreArrayList> pageableWrapper = wrapper2.getData();
        resultArrayList = pageableWrapper.getData();

        /**
         * selecting specific columns
         * */

        wrapper = Repository.select(StringRefs.SENTINEL, "master.users", "user_id, username");


        //The other Select Queries can be used in conjunction with Pagination or No Pagination as explained above. YOu can also provide the columns to select if you wish

        /**
         * Filtering
         * */

        wrapper = Repository.selectWhere(StringRefs.SENTINEL, "master.users",
                new FilterPredicate("user_id = :user_id"),
                new FlexicoreHashMap().addQueryArgument(":user_id", 1));

        /**
         * If you want to fetch only a single record, and you're sure that only a single record or more than one will be returned, you can simply do as below.
         * Will return record at index zero of the resulting ArrayList.
         */

        TransactionWrapper<FlexicoreHashMap> wrapperSingle = Repository.selectWhere(StringRefs.SENTINEL, "master.users",
                new FilterPredicate("user_id = :user_id"),
                new FlexicoreHashMap().addQueryArgument(":user_id", 1));

        FlexicoreHashMap flexicoreHashMap = wrapperSingle.getSingleRecord();

        /**
         * selecting specific columns
         * */

        wrapper = Repository.selectWhere(StringRefs.SENTINEL, "master.users", "DISTINCT user_id, username",
                new FilterPredicate("user_id = :user_id"),
                new FlexicoreHashMap().addQueryArgument(":user_id", 1));

        /**
         * selecting ordering by
         * */

        wrapper = Repository.selectWhereOrderBy(StringRefs.SENTINEL, "master.users", "DISTINCT user_id, username",
                new FilterPredicate("user_id = :user_id"),
                "date_created DESC",
                new FlexicoreHashMap().addQueryArgument(":user_id", 1));

        /**
         * selecting group by
         * */

        wrapper = Repository.selectWhereGroupBy(StringRefs.SENTINEL, "master.users", "DISTINCT user_id, username",
                new FilterPredicate("user_id = :user_id"),
                "date_created",
                new FlexicoreHashMap().addQueryArgument(":user_id", 1));

        /**
         * selecting group by order by
         * */

        wrapper = Repository.selectWhereGroupByOrderBy(StringRefs.SENTINEL, "master.users", "DISTINCT user_id, username",
                new FilterPredicate("user_id = :user_id"),
                "date_created",
                "date_created DESC",
                new FlexicoreHashMap().addQueryArgument(":user_id", 1));

        /**
         * check if exists
         * */

        boolean exists = Repository.exists(StringRefs.SENTINEL, "master.users",
                new FilterPredicate("user_id = :user_id"),
                new FlexicoreHashMap().addQueryArgument(":user_id", 1));

        /**
         * count records
         * */

        long count = Repository.countWhere(StringRefs.SENTINEL, "master.users",
                new FilterPredicate("date_created >= :date_created_1 AND date_created >= :date_created_2"),
                new FlexicoreHashMap()
                        .addQueryArgument(":date_created_1", "2023-04-01")
                        .addQueryArgument(":date_created_2", "2023-07-31")
        );


        /**
         * custom query. Can be as complex as you want. This is specifically for SELECTs
         * */

        QueryBuilder queryBuilder = new QueryBuilder()
                .rawQuery("SELECT * FROM master.users WHERE user_id = :user_id");

        wrapper = Repository.select(StringRefs.SENTINEL, queryBuilder,
                new FlexicoreHashMap().addQueryArgument(":user_id", 1));


        /**
         * JOINS. Pagination depends on whether you pass the array of page, pageSize. Don't manually call .limit() which is found in querybuilder.
         * */

        queryBuilder = new QueryBuilder()
                .select()
                .selectColumn("u.*, up.profile_name, o.organization_code, o.organization_name")
                .from()
                .joinPhrase("users u LEFT JOIN user_profiles up ON up.profile_code = :u.profile_code" +
                        "   LEFT JOIN organizations o ON u.organization_id = o.organization_id")
                .where("u.user_id = :user_id");

        wrapper2 = Repository.joinSelectQuery(StringRefs.SENTINEL, queryBuilder,
                new FlexicoreHashMap().addQueryArgument(":user_id", 1), new int[]{1,10});

    }

    private static void sampleInserts() {

        /**
         * INSERT SINGLE RECORD
         * */

        //Usually the values can come from the Request Body of an Endpoint, or you can manually create the insert values as below

        FlexicoreHashMap flexicoreHashMap = new FlexicoreHashMap();
        flexicoreHashMap.putValue("user_name", "john_doe");
        flexicoreHashMap.putValue("password", "5acfjffjfjff6a5565s4s6s");
        flexicoreHashMap.putValue("email_address", "john_doe@gmail.com");
        flexicoreHashMap.putValue("phone_number", "254712345678");
        flexicoreHashMap.putValue("login_attempts", 0);
        flexicoreHashMap.putValue("otp_attempts", 0);
        flexicoreHashMap.putValue("date_created", DateTime.getCurrentDateTime());
        flexicoreHashMap.putValue("date_modified", DateTime.getCurrentDateTime());

        //Name of function might be insertAutoIncremented, but it is still used for inserting even in tables which don't have auto_increment, like gender.
        TransactionWrapper<FlexicoreHashMap> wrapper = Repository.insertAutoIncremented(StringRefs.SENTINEL, "master.users",
                flexicoreHashMap);

        //Will return the inserted record including any generated fields e.g. auto increment values. This result is what you can send back as a response to an API call.
        flexicoreHashMap = wrapper.getSingleRecord();


        /**
         * INSERT BATCH RECORDS
         * */

        FlexicoreArrayList flexicoreArrayList = new FlexicoreArrayList();
        flexicoreArrayList.add(new FlexicoreHashMap()
                .putValue("user_name", "john_doe")
                .putValue("password", "5acfjffjfjff6a5565s4s6s")
                .putValue("email_address", "john_doe@gmail.com")
                .putValue("phone_number", "254712345678")
                .putValue("login_attempts", 0)
                .putValue("otp_attempts", 0)
                .putValue("date_created", DateTime.getCurrentDateTime())
                .putValue("date_modified", DateTime.getCurrentDateTime()));

        flexicoreArrayList.add(new FlexicoreHashMap()
                .putValue("user_name", "mary_doe")
                .putValue("password", "AAAAAAAAAAAAAAAAAAAAAAA")
                .putValue("email_address", "mary_doe@gmail.com")
                .putValue("phone_number", "254700000000")
                .putValue("login_attempts", 0)
                .putValue("otp_attempts", 0)
                .putValue("date_created", DateTime.getCurrentDateTime())
                .putValue("date_modified", DateTime.getCurrentDateTime()));

        TransactionWrapper<Boolean> wrapper2 = Repository.batchInsert(StringRefs.SENTINEL, "master.users",
                flexicoreArrayList);

        boolean isSuccessfulInsert = wrapper2.getData();


    }

    private static void sampleUpdate() {

        /**
         * UPDATE SPECIFIC RECORD(S)
         * */

        //Usually the values can come from the Request Body of an Endpoint, or you can manually create the insert values as below

        FlexicoreHashMap updateHashMap = new FlexicoreHashMap();
        updateHashMap.putValue("password", "5acfjffjfjff6a5565s4s6s");
        updateHashMap.putValue("email_address", "john_doe@gmail.com");
        updateHashMap.putValue("login_attempts", 0);
        updateHashMap.putValue("otp_attempts", 0);
        updateHashMap.putValue("date_modified", DateTime.getCurrentDateTime());

        TransactionWrapper<FlexicoreArrayList> wrapper = Repository.update(StringRefs.SENTINEL, "master.users",
                updateHashMap,
                new FilterPredicate("user_id = :user_id"),
                new FlexicoreHashMap().addQueryArgument(":user_id", 1));

        //Will return the updated records, including the old values before update in case you want to use them for audit_trail
        FlexicoreArrayList resultList = wrapper.getData();

        //if you don't want the old records to be returned, pass flag false as below:

        wrapper = Repository.update(StringRefs.SENTINEL, "master.users",
                updateHashMap,
                new FilterPredicate("user_id = :user_id"),
                new FlexicoreHashMap().addQueryArgument(":user_id", 1), false);

        /**
         * UPDATE ALL RECORD(S)
         * */

        //Updates require you pass the filter. If you want to update all records pass 1 = 1 as the filter i.e.

        wrapper = Repository.update(StringRefs.SENTINEL, "master.users", updateHashMap,
                new FilterPredicate("1 = 1"), null);
    }

    private static void sampleDeletes() {

        /**
         * DELETE SPECIFIC RECORD(S)
         * */

        TransactionWrapper<FlexicoreArrayList> wrapper = Repository.delete(StringRefs.SENTINEL, "master.users",
                new FilterPredicate("user_id = :user_id"),
                new FlexicoreHashMap().addQueryArgument(":user_id", 1));

        //Will return the deleted records in case you want to use them for audit_trail
        FlexicoreArrayList resultList = wrapper.getData();

        /**
         * DELETE ALL RECORD(S)
         * */

        //Deletes require you pass the filter. If you want to delete all records pass 1 = 1 as the filter i.e.

        wrapper = Repository.delete(StringRefs.SENTINEL, "master.users",  new FilterPredicate("1 = 1"), null);

    }
}

/* updateHashMap.put("ticket_stage","closed");
        updateHashMap.put("ticket_status","closed");

        QueryBuilder queryBuilder = new QueryBuilder()
                .rawQuery(
                        "UPDATE vendors.tickets\n" +
                                "SET ticket_status = 'closed',ticket_stage = 'closed'\n" +
                                "WHERE ticket_id = ANY(SELECT ticket_id  FROM vendors.tickets WHERE (NOW() - date_modified) > interval ':interval_string' AND ticket_stage = 'client-resolved');");

        TransactionWrapper<FlexicoreArrayList> wrapper2 = Repository.update(organizationId, queryBuilder,new FlexicoreHashMap().addQueryArgument(":ticket_ids", ));*/