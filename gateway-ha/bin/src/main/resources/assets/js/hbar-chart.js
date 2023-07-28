(function($) {
    "use strict";
    $.fn.hBarChart = function(customConfig) {
        var config = $.extend({
            bgColor: 'green',
            textColor: '#fff',
            show: 'label',
            sorting: true,
            maxStyle: {
                bg: 'orange',
                text: 'white'
            }
        }, customConfig);
        var chartObj = $(this);
        var data = [];
        var max = null;
        var bgColor = config.bgColor;
        var lightenDarkenColor = function(col, amt) {
            var usePound = false;
            if (col[0] == "#") {
                col = col.slice(1);
                usePound = true;
            }
            var num = parseInt(col, 16);
            var r = (num >> 16) + amt;
            if (r > 255) r = 255;
            else if (r < 0) r = 0;
            var b = ((num >> 8) & 0x00FF) + amt;
            if (b > 255) b = 255;
            else if (b < 0) b = 0;
            var g = (num & 0x0000FF) + amt;
            if (g > 255) g = 255;
            else if (g < 0) g = 0;
            return (usePound ? "#" : "") + (g | (b << 8) | (r << 16)).toString(16);
        }
        //end sorting operation
        if (config.sorting) {
            var sort_li = function(a, b) {
                return ($(b).data('data')) > ($(a).data('data')) ? 1 : -1;
            }
            chartObj.find("li").sort(sort_li) // sort elements
                .appendTo(chartObj); // append again to the list
        }
        //end sorting operation
        //global style
        chartObj.find("li").css({
            listStyleType: 'none',
            padding: '5px',
            boxSizing: 'border-box',
            marginTop: '3px',
            width: '100%',
            background: config.bgColor,
            color: config.textColor,
            whiteSpace: 'nowrap',
            borderRadius: '4px',
            fontSize: '13px',
            fontFamily: 'Tahoma, Geneva, sans-serif'
        })
        //global style
        //find max
        chartObj.find("li").each(function() {
            var val = $(this).data('data');
            data.push(val);
        });
        max = Math.max.apply(Math, data);
        // find max
        chartObj.find("li").each(function() {
            var lbl = $(this).text();
            var val = $(this).data('data');
            var percentage = (100 / max) * val;
            bgColor = lightenDarkenColor(bgColor, 10);
            // bar animation
            $(this).css({
                width: 0,
                background: bgColor
            });
            $(this).animate({
                width: percentage + '%'
            }, 1000)
            // bar animation
            //show label
            switch (config.show) {
                case 'label':
                    $(this).text(lbl);
                    break;
                case 'data':
                    $(this).text(val);
                    break;
                case 'both':
                    $(this).text(lbl).append(' (' + val + ')');
                    break;
                default:
                    break;
            }
            // end show label
            // max style
            if (val == max) {
                $(this).css({
                    background: config.maxStyle.bg,
                    color: config.maxStyle.text
                })
            }
            // max style
        });
    }
    //end plugin function
}(jQuery));