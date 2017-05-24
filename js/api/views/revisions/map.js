function (doc) {
    var org = doc.organization;

    if(doc.type && doc.type === 'Revision') {
        if(doc.attributes &&
            doc.attributes.file_id &&
            doc.attributes.previous) {
            emit([org, doc.attributes.file_id, doc.attributes.previous.length], null);
        }
    }
}
