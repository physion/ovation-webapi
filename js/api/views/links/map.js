/* Map function [entity URI,rel] -> [targets] */

function(doc) {
    var org = doc.organization;

    if (doc.type && doc.type === "Relation" && doc.user_id) {
        if (doc.name) {
            emit([org, doc.user_id, doc.source_id, doc.rel, doc.name], {'_id': doc.target_id});
        } else {
            emit([org, doc.user_id, doc.source_id, doc.rel], {'_id': doc.target_id});
        }
        if (doc.inverse_rel) {
            emit([org, doc.user_id, doc.target_id, doc.inverse_rel], {'_id': doc.source_id});
        }

        if (doc.links && doc.links._collaboration_roots) {
            var roots = doc.links._collaboration_roots;
            for (var i = 0; i < roots.length; i++) {
                if (doc.name) {
                    emit([org, roots[i], doc.source_id, doc.rel, doc.name], {'_id': doc.target_id});
                } else {
                    emit([org, roots[i], doc.source_id, doc.rel], {'_id': doc.target_id});
                }
                if (doc.inverse_rel) {
                    emit([org, roots[i], doc.target_id, doc.inverse_rel], {'_id': doc.source_id});
                }
            }
        }
    }
}
