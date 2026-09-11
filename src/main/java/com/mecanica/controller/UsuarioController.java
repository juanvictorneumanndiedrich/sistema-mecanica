package com.mecanica.controller;

import com.mecanica.dao.UsuarioDAO;
import com.mecanica.model.Usuario;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * Controller de Usuario: cadastro, edicao, permissoes e autenticacao
 * (login). A senha nunca e guardada em texto puro -- e sempre um hash
 * SHA-256 (suficiente pro escopo academico do projeto).
 */
public class UsuarioController {

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    /** Cadastra um usuario novo, ja recebendo a senha em texto puro pra fazer o hash. */
    public Usuario cadastrar(Usuario usuario, String senhaEmTextoPuro) {
        validarDadosBasicos(usuario);
        if (senhaEmTextoPuro == null || senhaEmTextoPuro.isBlank()) {
            throw new IllegalArgumentException("A senha e obrigatoria.");
        }
        if (usuarioDAO.buscarPorLogin(usuario.getLogin()) != null) {
            throw new IllegalArgumentException("Ja existe um usuario com esse login.");
        }
        usuario.setSenha(hashSenha(senhaEmTextoPuro));
        return usuarioDAO.salvar(usuario);
    }

    /**
     * Atualiza nome/login/permissoes/ativo de um usuario ja existente, sem
     * mexer na senha (pra isso, ver alterarSenha).
     */
    public Usuario atualizarDadosCadastrais(Usuario usuario) {
        validarDadosBasicos(usuario);
        return usuarioDAO.salvar(usuario);
    }

    public void alterarSenha(Usuario usuario, String novaSenhaEmTextoPuro) {
        if (novaSenhaEmTextoPuro == null || novaSenhaEmTextoPuro.isBlank()) {
            throw new IllegalArgumentException("A nova senha e obrigatoria.");
        }
        usuario.setSenha(hashSenha(novaSenhaEmTextoPuro));
        usuarioDAO.salvar(usuario);
    }

    /** Usado na tela de login. Retorna null se login/senha nao conferem ou o usuario esta inativo. */
    public Usuario autenticar(String login, String senhaEmTextoPuro) {
        Usuario usuario = usuarioDAO.buscarPorLogin(login);
        if (usuario == null || !usuario.isAtivo()) {
            return null;
        }
        return hashSenha(senhaEmTextoPuro).equals(usuario.getSenha()) ? usuario : null;
    }

    public Usuario buscarPorId(Long id) {
        return usuarioDAO.buscarPorId(id);
    }

    public List<Usuario> listarTodos() {
        return usuarioDAO.listarTodos();
    }

    public List<Usuario> listarAtivos() {
        return usuarioDAO.listarAtivos();
    }

    public void excluir(Usuario usuario) {
        usuarioDAO.excluir(usuario);
    }

    private void validarDadosBasicos(Usuario usuario) {
        if (usuario.getNome() == null || usuario.getNome().isBlank()) {
            throw new IllegalArgumentException("Nome do usuario e obrigatorio.");
        }
        if (usuario.getLogin() == null || usuario.getLogin().isBlank()) {
            throw new IllegalArgumentException("Login e obrigatorio.");
        }
    }

    private String hashSenha(String senhaEmTextoPuro) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(senhaEmTextoPuro.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Algoritmo de hash indisponivel.", e);
        }
    }
}