declare module 'sockjs-client' {
  class SockJS extends WebSocket {
    constructor(url: string, _reserved?: any, options?: any);
  }
  export default SockJS;
}
