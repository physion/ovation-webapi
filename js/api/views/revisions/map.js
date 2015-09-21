function (doc) {
    if (doc.type && doc.type === "Revision") {
        if (doc.attributes) {
            if (doc.attributes.resource && doc.attributes.previous) {
                emit(doc.attributes.resource, [[doc._id], doc.attributes.previous.length]);
            }

        }
    }
}
