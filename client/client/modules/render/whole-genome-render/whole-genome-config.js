const formatter = new Intl.NumberFormat('en-US');

export default {
    ticks: 50,
    start: {
        x: 10,
        y: 30,
        margin: 50,
        topMargin: 30,
    },
    axis: {
        color: 0x000000,
        thickness: 1,
        offsetX: 0,
        width: 50
    },
    tick: {
        formatter: ::formatter.format,
        thickness: 1,
        offsetXOdd: 15,
        offsetXEven: 10,
        color: 0x777777,
        margin: 4,
        label: {
            fill: 0x000000,
            font: 'normal 7pt arial',
        }
    },
    chromosomeColumn: {
        spaceBetween: 70,
        margin:5,
        topMargin: 20,
        width: 15,
        lineColor: 0x000000,
        fill: 0xadadad,
        thickness: 1
    },
    gridSize: 5,
    hit: {
        width: 3,
        lineColor: 0x32cd32,
        offset: 5,
    }
};