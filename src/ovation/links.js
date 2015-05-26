/* Map function [entity URI,rel] -> [targets] */
function links(doc) {
    if (doc.type && doc.type === "Relation") {
        if (doc.label) {
            emit([doc.source_id, doc.rel, doc.label], {'_id': doc.target_id});
        } else {
            emit([doc.source_id, doc.rel], {'_id': doc.target_id});
        }

        if (doc.inverse_rel) {
            emit([doc.target_id, doc.inverse_rel], {'_id': doc.source_id});
        }
    }
}
