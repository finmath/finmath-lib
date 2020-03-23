var clover = new Object();

// JSON: {classes : [{name, id, sl, el,  methods : [{sl, el}, ...]}, ...]}
clover.pageData = {"classes":[{"el":50,"id":249,"methods":[{"el":17,"sc":2,"sl":13},{"el":26,"sc":2,"sl":23},{"el":35,"sc":2,"sl":32},{"el":48,"sc":2,"sl":37}],"name":"BoundConstraint","sl":9}]}

// JSON: {test_ID : {"methods": [ID1, ID2, ID3...], "name" : "testXXX() void"}, ...};
clover.testTargets = {"test_102":{"methods":[{"sl":13},{"sl":23},{"sl":32},{"sl":37}],"name":"test","pass":true,"statements":[{"sl":14},{"sl":15},{"sl":16},{"sl":25},{"sl":34},{"sl":40},{"sl":41},{"sl":43},{"sl":46}]},"test_63":{"methods":[{"sl":13},{"sl":23},{"sl":32},{"sl":37}],"name":"test","pass":true,"statements":[{"sl":14},{"sl":15},{"sl":16},{"sl":25},{"sl":34},{"sl":40},{"sl":41},{"sl":43},{"sl":46}]}}

// JSON: { lines : [{tests : [testid1, testid2, testid3, ...]}, ...]};
clover.srcFileLines = [[], [], [], [], [], [], [], [], [], [], [], [], [], [63, 102], [63, 102], [63, 102], [63, 102], [], [], [], [], [], [], [63, 102], [], [63, 102], [], [], [], [], [], [], [63, 102], [], [63, 102], [], [], [63, 102], [], [], [63, 102], [63, 102], [], [63, 102], [], [], [63, 102], [], [], [], []]