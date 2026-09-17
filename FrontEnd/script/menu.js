fetch('menu.html')
    .then(response => response.text())
    .then(data => {

        // menu.html traz DOIS blocos juntos (sidebar + topbar).
        // Em vez de jogar tudo dentro de uma única div (o que juntava
        // os dois lado a lado com o conteúdo da página), a gente separa
        // cada bloco e encaixa no slot correto de CADA página.
        const temp = document.createElement('div');
        temp.innerHTML = data;

        const sidebar = temp.querySelector('#sidebar');
        const topbar = temp.querySelector('.topbar');

        document.getElementById('sidebar-slot').replaceWith(sidebar);
        document.getElementById('topbar-slot').replaceWith(topbar);


        // ABRIR E FECHAR SIDEBAR

        document.getElementById('open-btn').addEventListener('click', function () {

            document.getElementById('sidebar').classList.toggle('open-sidebar');

        });


        // TÍTULO DA PÁGINA

        const linksMenu = document.querySelectorAll('.side-item a');
        const tituloPagina = document.getElementById('titulo-pagina');

        linksMenu.forEach(link => {

            link.addEventListener('click', function (event) {

                event.preventDefault();

                tituloPagina.textContent = this.dataset.title;

            });

        });

    });

// CARREGAR POPUPS    

fetch('popups.html')
    .then(response => response.text())
    .then(data => {

        document.getElementById('popup-slot').innerHTML = data;

    });    

function openPopup(id){ document.getElementById(id).classList.add('active'); }
function closePopup(id){ document.getElementById(id).classList.remove('active'); }
function openPopupSair(id){ document.getElementById(id).classList.add('active'); }
function closeOnBackdrop(ev,id){ if(ev.target.id===id) closePopup(id); }    