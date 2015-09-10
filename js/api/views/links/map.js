/* Map function [entity URI,rel] -> [targets] */

function(doc) {
    if (doc.type && doc.type === "Relation") {
        if (doc.name) {
            emit(["ovation://entities/" + doc.source_id, doc.rel, doc.name], {'_id': doc.target_id});
            emit([doc.source_id, doc.rel, doc.name], {'_id': doc.target_id});
        } else {
            emit(["ovation://entities/" + doc.source_id, doc.rel], {'_id': doc.target_id});
            emit([doc.source_id, doc.rel], {'_id': doc.target_id});
        }
        if (doc.inverse_rel) {
            emit(["ovation://entities/" + doc.target_id, doc.inverse_rel], {'_id': doc.source_id});
            emit([doc.target_id, doc.inverse_rel], {'_id': doc.source_id});
        }
    }
}
