
// modules/jsqtk.js

jsqtk = {
    blackList: [],
    keys: [],
    result: {},
    resultCounter: {},
    idCounter: 0,

    info: function (id) {
        console.log('jsqtk.info()')
        let elem = $('*[data-jsqtk-id="jsqtk-' + id + '"]')

        if (elem) {
            console.log(elem)
            let evs = $._data(elem[0], 'events')
            $.each(evs, function (i, elist) {
                $.each(elist, function (ii, oo) {
                    console.log(oo)
                })
            })
        }
    },

    history: function () {
        console.log('jsqtk.history()')
        $.each(jsqtk.keys, function (i, e) {
            console.log(e)
        })
    },

    _check: function (events) {
        let result = []

        for (let i=0; i<events.length; i++) {
            for (let j=i+1; j<events.length; j++) {
                if (events[i].handler.toString() == events[j].handler.toString()) {
                    if ($.inArray(events[i].handler, result) < 0) {
                        result.push(events[i].handler)
                    }
                    if ($.inArray(events[j].handler, result) < 0) {
                        result.push(events[j].handler)
                    }
                }
            }
        }
        return result
    },

    go: function () {
        // console.log('jsqtk.go()')
        let evsCounter = 0

        jsqtk.result = {}
        jsqtk.resultCounter = {}

        $.each($('*'), function (i, elem) {
            jsqtk.idCounter = jsqtk.idCounter + 1

            let jsqtkEid = 'jsqtk-' + jsqtk.idCounter
            let evs = $._data(elem, 'events')

            if (evs) {
                evsCounter = evsCounter + Object.keys(evs).length

                $.each(evs, function (ii, elist) {
                    if ($.inArray(ii, jsqtk.blackList) < 0 && elist.length > 1) {
                        let checkList = jsqtk._check(elist)

                        if (checkList.length > 0) {
                            if ($(elem).attr('data-jsqtk-id')) {
                                jsqtkEid = $(elem).attr('data-jsqtk-id')
                            } else {
                                $(elem).attr('data-jsqtk-id', jsqtkEid)
                            }

                            if (!jsqtk.result[jsqtkEid]) {
                                jsqtk.result[jsqtkEid] = [elem]
                            }
                            jsqtk.result[jsqtkEid][ii] = checkList

                            if (!jsqtk.resultCounter[ii]) {
                                jsqtk.resultCounter[ii] = 0
                            }
                            jsqtk.resultCounter[ii]++
                        }
                    }
                })
            }
        })

        let keys = Object.keys(jsqtk.result).sort(function (a, b) {
            a.split('-')[1] < b.split('-')[1] ? 1 : -1
        })
        jsqtk.keys.push(keys)

        console.log('¯\\_(ツ)_/¯ .. jsqtk')
        console.log('- event listeners found overall: ' + evsCounter)
        if (jsqtk.blackList.length > 0) {
            console.log('- blacklist: ' + jsqtk.blackList)
        }
        if (keys.length > 0) {
            let history = []
            jsqtk.keys.forEach(function(i){ history.push(i.length) })

            console.log('- data-jsqtk-ids in use: ' + keys)
            console.log('- history of used data-jsqtk-ids: [' + history + ']')
            console.log('- el doublets found: %o', jsqtk.resultCounter)
            console.groupCollapsed('- elements with el doublets: ' + Object.keys(jsqtk.result).length)
            keys.forEach(function (k) {
                console.log(jsqtk.result[k])
            })
            console.groupEnd()
        }
    }
}