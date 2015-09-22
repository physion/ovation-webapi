function (doc) {
    if (doc.type && doc.type === "Revision") {
        if (doc.attributes) {
            if (doc.attributes.file_id && doc.attributes.previous) {
                emit(doc.attributes.file_id, [[doc._id], doc.attributes.previous.length]);
            }

        }
    }
}
