/**
 * Created by barry on 9/4/15.
 */
function (doc) {
    var org = doc.organization;

    if (doc.type && doc.type === "Relation" && doc.user_id) {
        emit([org, doc.user_id, doc.source_id, doc.rel], null);
        if (doc.inverse_rel) {
            emit([org, doc.user_id, doc.target_id, doc.inverse_rel], null);
        }

        if (doc.links && doc.links._collaboration_roots) {
            var roots = doc.links._collaboration_roots;
            for (var i = 0; i < roots.length; i++) {
                emit([org, roots[i], doc._id], null);
                emit([org, roots[i], doc.source_id, doc.rel], null);
                if (doc.inverse_rel) {
                    emit([org, roots[i], doc.target_id, doc.inverse_rel], null);
                }
            }
        }
    }
}
