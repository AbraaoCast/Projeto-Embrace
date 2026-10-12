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


        // Deixa a opção do menu sidebar ativa de acordo com a pagina que está sendo acessada
        const paginaAtual = document.body.dataset.page;
        document.querySelectorAll('.side-item').forEach(item => {
            if (item.dataset.page === paginaAtual) {
        item.classList.add('active');
            }
        });


        // DADOS DO ADMINISTRADOR AUTENTICADO

        fetch('api/session')
            .then(response => {
                if (!response.ok) {
                    throw new Error('Sessão indisponível');
                }
                return response.json();
            })
            .then(administrador => {
                document.getElementById('nome-admin').textContent = administrador.nome;
            })
            .catch(() => {
                window.location.href = 'login-cadas.html?error=session_required';
            });


        
        // ABRIR E FECHAR SIDEBAR

        const sidebarAtual = document.getElementById('sidebar');

        // Na primeira abertura do sistema, inicia aberto.
        // Nas próximas páginas, recupera o estado anterior.
        const estadoSidebar = sessionStorage.getItem('sidebarAberto');

        if (estadoSidebar === null || estadoSidebar === 'true') {
            sidebarAtual.classList.add('open-sidebar');
        } else {
            sidebarAtual.classList.remove('open-sidebar');
        }

        document.getElementById('open-btn').addEventListener('click', function () {
            sidebarAtual.classList.toggle('open-sidebar');

            // Salva o estado para manter ao navegar entre páginas.
            sessionStorage.setItem(
                'sidebarAberto',
                sidebarAtual.classList.contains('open-sidebar')
            );
        });
    


        // TÍTULO DA PÁGINA

        const tituloPagina = document.getElementById('titulo-pagina');

        tituloPagina.textContent = document.title;

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
