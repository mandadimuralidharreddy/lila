var m = require('mithril');
var classSet = require('common').classSet;
var plural = require('../../util').plural;

var firstRender = true;

function selector(data) {
  if (!firstRender && m.redraw.strategy() === 'diff') return {
    subtree: 'retain'
  };
  firstRender = false;
  return m('select.selector', {
    onchange: function(e) {
      location.href = '/practice/' + e.target.value;
    }
  }, [
    m('option[disabled][selected]', 'Practice list'),
    data.structure.map(function(section) {
      return m('optgroup', {
        label: section.name
      }, section.studies.map(function(study) {
        return m('option', {
          value: '/' + section.id + '/' + study.slug + '/' + study.id
        }, study.name);
      }));
    })
  ]);
};

function renderGoal(practice) {
  var moves = practice.goal().moves - practice.nbMoves();
  switch (practice.goal().result) {
    case 'mate':
      return 'Checkmate the opponent';
    case 'mateIn':
      return 'Checkmate the opponent in ' + plural('move', moves);
    case 'drawIn':
      return 'Hold the draw for ' + plural('more move', moves);
    case 'evalIn':
      return 'Get a winning position in ' + plural('move', moves);
  }
}

module.exports = {
  underboard: function(ctrl) {
    var p = ctrl.practice;
    if (p.won()) {
      var next = p.nextChapter();
      return m('a.feedback.complete', {
        onclick: function() {
          ctrl.setChapter(next.id);
        }
      }, [
        m('span', 'Success!'),
        'Next: ',
        m('strong', next.name)
      ]);
    }
    return m('div.feedback.ongoing', [
      m('div.goal', [
        'Your goal: ',
        m('strong', renderGoal(p))
      ]),
      p.comment() ? m('div.comment', p.comment()) : null
    ]);
  },
  main: function(ctrl) {

    var current = ctrl.currentChapter();
    var data = ctrl.practice.data;

    return [
      m('div.title', [
        m('i.practice.icon.' + data.study.id),
        m('div.text', [
          m('h1', data.study.name),
          m('em', data.study.desc)
        ])
      ]),
      m('div', {
        key: 'chapters',
        class: 'list chapters',
        config: function(el, isUpdate) {
          if (!isUpdate)
            el.addEventListener('click', function(e) {
              var id = e.target.parentNode.getAttribute('data-id') || e.target.getAttribute('data-id');
              if (id) ctrl.setChapter(id);
            });
        }
      }, [
        ctrl.chapters.list().map(function(chapter, i) {
          var loading = ctrl.vm.loading && chapter.id === ctrl.vm.nextChapterId;
          var active = !ctrl.vm.loading && current && current.id === chapter.id;
          var completion = data.completion[chapter.id] ? 'done' : 'ongoing';
          return [
            m('div', {
              key: chapter.id,
              'data-id': chapter.id,
              class: 'elem chapter ' + classSet({
                active: active,
                loading: loading
              })
            }, [
              m('span', {
                'data-icon': ((loading || active) && completion === 'ongoing') ? 'G' : 'E',
                class: 'status ' + completion
              }),
              m('h3', chapter.name)
            ])
          ];
        })
      ]),
      m('div.finally', [
        m('a.back', {
          'data-icon': 'I',
          href: '/practice',
          title: 'More practice'
        }),
        selector(data)
      ])
    ];
  }
};
