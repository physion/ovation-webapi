/*View mapping function to get documents by org and ID*/

function(doc) {
    if('organization' in doc) {
        var org = doc.organization;
        emit([org, doc._id], null);
    }
}
