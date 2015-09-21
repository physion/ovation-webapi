function (keys, values, rereduce) {

    return values.reduce(function (prev, current, index, arr) {
        ids = prev[0];
        max = prev[1];

        if (current[1] === max) {
            for (i = 0; i < current[0].length; i++) {
                ids.push(current[0][i]);
            }
        } else if (current[1] > max) {
            max = current[1];
            ids = current[0];
        }

        return [ids, max];
    }, [[], 0]);
}
