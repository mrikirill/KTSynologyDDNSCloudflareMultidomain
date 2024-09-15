package domian
/*
 * Output:
 *    When you write your own module, you can use the following words to tell user what happen by print it.
 *    You can use your own message, but there is no multiple-language support.
 *
 *       good -  Update successfully.
 *       nochg - Update successfully but the IP address have not changed.
 *       nohost - The hostname specified does not exist in this user account.
 *       abuse - The hostname specified is blocked for update abuse.
 *       notfqdn - The hostname specified is not a fully-qualified domain name.
 *       badauth - Authenticate failed.
 *       911 - There is a problem or scheduled maintenance on provider side
 *       badagent - The user agent sent bad request(like HTTP method/parameters is not permitted)
 *       badresolv - Failed to connect to  because failed to resolve provider address.
 *       badconn - Failed to connect to provider because connection timeout.
 */

object SynologyOutput {
    const val SUCCESS = "good"               // Update successfully
    const val NO_HOSTNAME = "nohost"         // The hostname specified does not exist in this user account
    const val HOSTNAME_INCORRECT = "notfqdn" // The hostname specified is not a fully-qualified domain name
    const val AUTH_FAILED = "badauth"        // Authenticate failed
    const val DDNS_FAILED = "911"            // There is a problem or scheduled maintenance on provider side
    const val BAD_HTTP_REQUEST = "badagent"  // HTTP method/parameters is not permitted
    const val BAD_PARAMS = "badparam"        // Bad params
}