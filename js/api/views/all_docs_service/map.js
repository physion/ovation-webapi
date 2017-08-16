/*View mapping function to get documents by org and ID*/

function(doc) {
    if(doc.organization) {
        var org = doc.organization;
        emit([org, doc._id], null);
    }
}
