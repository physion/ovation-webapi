function (doc) {
    if(doc.type && doc.type === 'Revision') {
        if(doc.attributes &&
            doc.attributes.file_id &&
            doc.attributes.previous) {
            emit([doc.attributes.file_id, doc.attributes.previous.length], null);
        }
    }
}
